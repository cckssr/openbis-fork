#
#   Copyright ETH 2018 - 2026 Zürich, Scientific IT Services
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
"""Parallel chunked download protocol against the openBIS fileserver."""

import binascii
import functools
import json
import os
import time
from pathlib import Path
from threading import Lock, Thread
from typing import Any, Optional
from urllib.parse import urljoin

import requests
from requests.adapters import HTTPAdapter
from urllib3.util import Retry

DSS_V3 = "/datastore_server/rmi-data-store-server-v3.json"
REQUEST_RETRIES_COUNT = 3
DOWNLOAD_RETRIES_COUNT = 3
FAST_DOWNLOAD_PROTOCOL_VERSION = 2


def make_fileserver_body_params(
    server_information: Any, **params: Any
) -> "dict[str, list[str]]":
    """Create a proper map of key-values for a fileserver request."""
    result = {}
    if server_information.is_version_greater_than(3, 6):
        result = {"version": [str(FAST_DOWNLOAD_PROTOCOL_VERSION)]}

    for key, value in params.items():
        result[str(key)] = [str(value).replace("'", '"')]
    return result


def comma_separated_items(arr: Any) -> str:
    """Create comma-separated string from the list of items."""
    return str(functools.reduce(lambda a, b: a + ", " + b, arr))


def create_session(download_url_base: str) -> requests.Session:
    """Create a session object to handle retries in case of server failure."""
    session = requests.Session()
    retries = Retry(
        total=REQUEST_RETRIES_COUNT, backoff_factor=1, status_forcelist=[502, 503, 504]
    )
    session.mount(download_url_base, HTTPAdapter(max_retries=retries))
    return session


def post_request(
    session: requests.Session,
    full_url: str,
    verify_certificates: bool,
    request: Any,
    parse_response: bool = True,
) -> Any:
    """Perform POST call to server."""
    try:
        if request:
            resp = session.post(
                full_url, json.dumps(request), verify=verify_certificates
            )
        else:
            resp = session.post(full_url, verify=verify_certificates)
    except requests.exceptions.SSLError as exc:
        raise requests.exceptions.SSLError(
            "Certificate validation failed. Use o=Openbis(url, verify_certificates=False) if you are using self-signed certificates."
        ) from exc
    except requests.ConnectionError as exc:
        raise requests.ConnectionError(
            "Could not connecto to the openBIS server. Please check your internet connection, the specified hostname and port."
        ) from exc
    if resp.ok:
        if parse_response is True:
            resp = resp.json()
            if "error" in resp:
                print(json.dumps(resp))
                raise ValueError(resp["error"]["message"])
        return resp
    else:
        raise ValueError("general error while performing post request")


def queue_chunks(
    session: requests.Session,
    base_url: str,
    download_session_id: str,
    chunks: "list[str]",
    verify_certificates: bool,
    server_information: Any,
) -> None:
    """Queue particular session chunks for download."""
    queue_request = make_fileserver_body_params(
        server_information,
        method="queue",
        downloadSessionId=download_session_id,
        ranges=comma_separated_items(chunks),
    )
    response = post_request(
        session, base_url, verify_certificates, queue_request, parse_response=False
    )
    if response.ok is False:
        raise ValueError(
            "error during queueing for download! Message:"
            + response["error"]["message"]
        )


def deserialize_chunk(byte_array: bytes) -> "dict[str, Any]":
    """Parse one binary chunk into its header fields and payload."""
    sequence_number_bytes = 4
    download_item_id_length_bytes = 2
    is_directory_bytes = 1
    file_path_length_bytes = 2
    file_offset_bytes = 8
    payload_length_bytes = 4
    sent_header_checksum_bytes = 8
    sent_payload_checksum_bytes = 8

    result: dict[str, Any] = {"invalid": False, "invalid_reason": ""}

    if len(byte_array) == 0:
        result["invalid"] = True
        result["invalid_reason"] = "HEADER"
        return result

    start, end = 0, sequence_number_bytes
    result["sequence_number"] = int.from_bytes(byte_array[start:end], "big")
    start, end = end, end + download_item_id_length_bytes
    download_item_id_length = int.from_bytes(byte_array[start:end], "big")
    start, end = end, end + is_directory_bytes
    result["is_directory"] = bool.from_bytes(byte_array[start:end], "big")
    start, end = end, end + file_path_length_bytes
    file_path_length = int.from_bytes(byte_array[start:end], "big")
    start, end = end, end + file_offset_bytes
    result["file_offset"] = int.from_bytes(byte_array[start:end], "big")
    start, end = end, end + payload_length_bytes
    result["payload_length"] = int.from_bytes(byte_array[start:end], "big")
    start, end = end, end + download_item_id_length
    result["download_item_id"] = byte_array[start:end].decode("utf8")
    start, end = end, end + file_path_length
    result["file_path"] = byte_array[start:end].decode("utf8")
    calculated_header_checksum = binascii.crc32(byte_array[:end])
    # End of header
    start, end = end, end + sent_header_checksum_bytes
    sent_header_checksum = int.from_bytes(byte_array[start:end], "big")
    if sent_header_checksum != calculated_header_checksum:
        result["invalid"] = True
        result["invalid_reason"] = "HEADER"
        return result
    start, end = end, end + result["payload_length"]
    result["payload"] = byte_array[start:end]
    calculated_payload_checksum = binascii.crc32(byte_array[start:end])
    # End of payload
    start, end = end, end + sent_payload_checksum_bytes
    sent_payload_checksum = int.from_bytes(byte_array[start:end], "big")
    if sent_payload_checksum != calculated_payload_checksum:
        result["invalid"] = True
        result["invalid_reason"] = "PAYLOAD"
        return result

    return result


class AtomicChecker:
    """Helper class for keeping watch of chunks to download."""

    def __init__(self, values_to_download: "set[int]") -> None:
        """Track the chunk numbers that still need downloading."""
        self._value = 0
        self._max = len(
            values_to_download
        )  # limit to not have an infinite download sessions
        self._set = values_to_download
        self._lock = Lock()

    def should_continue(self) -> bool:
        """Claim one download slot; False when the budget is exhausted."""
        with self._lock:
            if self._value >= self._max:
                return False
            self._value += 1
            return True

    def repeat_call(self) -> None:
        """Grow the budget by one (a chunk was re-queued)."""
        with self._lock:
            self._max += 1

    def break_count(self) -> None:
        """Abort: no further downloads are allowed."""
        with self._lock:
            self._max = 0

    def remove_value(self, value: int) -> None:
        """Mark one chunk as successfully downloaded."""
        with self._lock:
            if value in self._set:
                self._set.remove(value)

    def get_remaining_values(self) -> "set[int]":
        """Return the chunks that are still missing."""
        return self._set


def _get_json(response: Any) -> "tuple[bool, Any]":
    try:
        return True, response.json()
    except:
        return False, response


class DownloadThread(Thread):
    """Helper class defining single stream download."""

    def __init__(
        self,
        session: requests.Session,
        download_url_base: str,
        download_session_id: str,
        stream_id: str,
        counter: AtomicChecker,
        verify_certificates: bool,
        create_default_folders: bool,
        destination: str,
        server_information: Any,
        number_of_chunks: int = 1,
    ) -> None:
        """Set up one download stream worker."""
        Thread.__init__(self)
        self.session = session
        self.download_url = download_url_base
        self.download_session_id = download_session_id
        self.stream_id = stream_id
        self.counter = counter
        self.number_of_chunks = number_of_chunks
        self.create_default_folders = create_default_folders
        self.destination = destination
        self.verify_certificates = verify_certificates
        self.exc: Optional[Exception] = None
        self.server_information = server_information

    def run(self) -> None:
        """Download chunks until the shared budget is exhausted."""
        repeated_chunks: dict[int, int] = {}
        download_params = make_fileserver_body_params(
            self.server_information,
            method="download",
            downloadSessionId=self.download_session_id,
            numberOfChunks=self.number_of_chunks,
            downloadStreamId=self.stream_id,
        )
        retry_counter = 0
        while self.counter.should_continue():
            try:
                download_response = self.session.post(
                    self.download_url,
                    data=json.dumps(download_params),
                    stream=True,
                    verify=self.verify_certificates,
                )
                if download_response.ok is True:
                    data = deserialize_chunk(download_response.content)
                    if data["invalid"] is True:
                        is_json, response = _get_json(download_response)
                        if is_json:
                            if (
                                "retriable" in response
                                and response["retriable"] is False
                            ):
                                self.counter.break_count()
                                raise ValueError(response["error"])
                        else:
                            if data["invalid_reason"] == "PAYLOAD":
                                sequence_number = data["sequence_number"]
                                if (
                                    repeated_chunks.get(sequence_number, 0)
                                    >= DOWNLOAD_RETRIES_COUNT
                                ):
                                    self.counter.break_count()
                                    raise ValueError(
                                        "Received incorrect payload multiple times. Aborting."
                                    )
                                repeated_chunks[sequence_number] = (
                                    repeated_chunks.get(sequence_number, 0) + 1
                                )
                                queue_chunks(
                                    self.session,
                                    self.download_url,
                                    self.download_session_id,
                                    [f"{sequence_number}:{sequence_number}"],
                                    self.verify_certificates,
                                    self.server_information,
                                )
                                self.counter.repeat_call()  # queue additional download chunk run

                        if retry_counter >= REQUEST_RETRIES_COUNT:
                            self.counter.break_count()
                            raise ValueError(
                                "Consecutive download calls to the server failed."
                            )

                        # Exponential backoff for the consecutive failures
                        time.sleep(2**retry_counter)
                        retry_counter += 1

                    else:
                        retry_counter = 0
                        sequence_number = data["sequence_number"]
                        self.save_to_file(data)
                        self.counter.remove_value(sequence_number)
            except Exception as e:
                self.exc = e

    def save_to_file(self, deserialized_response: "dict[str, Any]") -> None:
        """Write one chunk's payload at its offset in the target file."""
        file_name = deserialized_response["file_path"]
        if self.create_default_folders:
            # create original/ or original/DEFAULT subdirectories
            filename_dest = os.path.join(self.destination, file_name)
        else:
            # ignore original/ and original/DEFAULT folders that come from openBIS
            if file_name.startswith("original/"):
                file_name = file_name.replace("original/", "", 1)
            if file_name.startswith("DEFAULT/"):
                file_name = file_name.replace("DEFAULT/", "", 1)
            filename_dest = os.path.join(self.destination, file_name)

        # create the necessary directory structure if they don't exist yet
        os.makedirs(os.path.dirname(filename_dest), exist_ok=True)

        if deserialized_response["is_directory"] is False:
            # create file if it does not exist already
            Path(filename_dest).touch(exist_ok=True)

            file_offset = deserialized_response["file_offset"]
            with open(filename_dest, "rb+") as file:
                file.seek(file_offset)
                file.write(deserialized_response["payload"])


class FastDownload:
    """Class for downloading data using FastDownload scheme."""

    def __init__(
        self,
        token: Optional[str],
        download_url: str,
        perm_id: str,
        files: Any,
        destination: str,
        create_default_folders: bool,
        wait_until_finished: bool,
        verify_certificates: bool,
        server_information: Any,
        wished_number_of_streams: int = 4,
    ) -> None:
        """Store the download parameters and create the retrying session."""
        self.dss_facade_url = urljoin(download_url, DSS_V3)
        self.session = create_session(download_url)
        self.token = token
        self.verify_certificates = verify_certificates
        self.perm_id = perm_id
        self.destination = destination
        self.create_default_folders = create_default_folders
        self.wait_until_finished = wait_until_finished
        self.wished_number_of_streams = wished_number_of_streams
        self.server_information = server_information

        if files is None:
            raise ValueError("please provide at least one file")

        if isinstance(files, str):
            files = [files]

        self.files = files

    def download(self) -> str:
        """Fast download of files from dataset."""
        if self.token is None:
            raise ValueError("Your session expired, please log in again")

        # Step 1 - request DSS Facade to create fast download session in fileserver

        create_fast_download_response = post_request(
            self.session,
            self.dss_facade_url,
            self.verify_certificates,
            self._create_fast_download_session_request(),
        )["result"]
        download_url = create_fast_download_response["downloadUrl"]
        user_session_id = create_fast_download_response["fileTransferUserSessionId"]

        # Step 2 - Request fileserver to start the download session

        download_item_ids: Any
        if self.server_information.is_version_greater_than(3, 6):
            download_item_ids = list(
                map(
                    lambda file: file["filePath"],
                    create_fast_download_response["files"],
                )
            )
        else:
            download_item_ids = comma_separated_items(
                map(
                    lambda file: file["filePath"],
                    create_fast_download_response["files"],
                )
            )

        start_session_params = make_fileserver_body_params(
            self.server_information,
            method="startDownloadSession",
            userSessionId=user_session_id,
            downloadItemIds=download_item_ids,
            wishedNumberOfStreams=self.wished_number_of_streams,
        )

        start_download_session = post_request(
            self.session, download_url, self.verify_certificates, start_session_params
        )
        download_session_id = start_download_session["downloadSessionId"]

        # Step 3 - Put files into fileserver download queue

        ranges = start_download_session["ranges"]
        self._queue_all_files(download_url, download_session_id, ranges)

        # Step 4 & 5 - Download files in chunks and close connection

        session_stream_ids = list(start_download_session["streamIds"])

        exception_list: list[Exception] = []
        thread = Thread(
            target=self._download_step,
            args=(
                download_url,
                download_session_id,
                session_stream_ids,
                ranges,
                exception_list,
            ),
        )
        thread.start()

        if self.wait_until_finished is True:
            thread.join()
            if exception_list:
                raise exception_list[0]

        return self.destination

    def _create_fast_download_session_request(self) -> "dict[str, Any]":
        file_ids = list(
            map(lambda file_path: self._make_json_id(file_path), self.files)
        )

        fast_download_session_options = {
            "wishedNumberOfStreams": self.wished_number_of_streams,
            "@type": "dss.dto.datasetfile.fastdownload.FastDownloadSessionOptions",
        }

        return {
            "id": "2",
            "jsonrpc": "2.0",
            "method": "createFastDownloadSession",
            "params": [self.token, file_ids, fast_download_session_options],
        }

    def _make_json_id(self, file_path: str) -> "dict[str, Any]":
        """Prepare JSON to create session for fileserver for given file in dataset."""
        return {
            "dataSetId": {
                "permId": self.perm_id,
                "@type": "as.dto.dataset.id.DataSetPermId",
            },
            "filePath": self.perm_id + "/" + file_path,
            "@type": "dss.dto.datasetfile.id.DataSetFilePermId",
        }

    def _queue_all_files(
        self, base_url: str, download_session_id: str, ranges: "dict[str, str]"
    ) -> None:
        """Queue all chunks for download from the fileserver.

        Each file receives a different chunk range, e.g. FileA ``0:4``,
        FileB ``5:6``.
        """
        chunks: list[str] = []
        for file, chunks_range in ranges.items():
            chunks += [chunks_range]
        queue_chunks(
            self.session,
            base_url,
            download_session_id,
            chunks,
            self.verify_certificates,
            self.server_information,
        )

    def _download_step(
        self,
        download_url: str,
        download_session_id: str,
        session_stream_ids: "list[str]",
        ranges: "dict[str, str]",
        exception_list: "list[Exception]",
    ) -> None:
        """Perform downloading of chunks in separate threads.

        Args:
            download_url: URL to use for downloading data.
            download_session_id: Download session id.
            session_stream_ids: List of available streams.
            ranges: Chunk ranges provided per file.
            exception_list: Output list collecting per-stream failures.
        """
        min_chunk = min(map(lambda x: int(x.split(":")[0]), ranges.values()))
        max_chunk = max(map(lambda x: int(x.split(":")[1]), ranges.values()))
        chunks_to_download = set(range(min_chunk, max_chunk + 1))

        counter = 1
        try:
            while True:  # each iteration will create threads for streams
                checker = AtomicChecker(chunks_to_download)
                streams = [
                    DownloadThread(
                        self.session,
                        download_url,
                        download_session_id,
                        stream_id,
                        checker,
                        self.verify_certificates,
                        self.create_default_folders,
                        self.destination,
                        self.server_information,
                    )
                    for stream_id in session_stream_ids
                ]

                for thread in streams:
                    thread.start()
                for thread in streams:
                    thread.join()

                if (
                    chunks_to_download == set()
                ):  # if there are no more chunks to download
                    break
                else:
                    if counter >= DOWNLOAD_RETRIES_COUNT:
                        print(f"Reached maximum retry count:{counter}. Aborting.")
                        exception_list += [
                            ValueError(
                                f"Reached maximum retry count:{counter}. Aborting."
                            )
                        ]
                        break
                    exceptions = [
                        stream.exc for stream in streams if stream.exc is not None
                    ]
                    if exceptions:
                        print(f"Download failed with message: {exceptions[0]}")
                        exception_list += exceptions
                        break
                    counter += 1
                    # queue chunks that failed to download in the previous pass
                    queue_chunks(
                        self.session,
                        download_url,
                        download_session_id,
                        [f"{x}:{x}" for x in chunks_to_download],
                        self.verify_certificates,
                        self.server_information,
                    )
        finally:
            # Step 5 - Close the session
            finish_download_session_params = make_fileserver_body_params(
                self.server_information,
                method="finishDownloadSession",
                downloadSessionId=download_session_id,
            )

            self.session.post(
                download_url,
                data=json.dumps(finish_download_session_params),
                verify=self.verify_certificates,
            )
