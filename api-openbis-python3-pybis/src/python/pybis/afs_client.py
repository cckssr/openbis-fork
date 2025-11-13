
import requests
import os
import json

import urllib.parse
from queue import Queue, Empty
import threading
from threading import Thread
from typing import Set, Optional, List
from urllib.parse import urljoin, quote

import requests
from pandas import DataFrame
from requests import Session
from requests.adapters import HTTPAdapter, Retry
from tabulate import tabulate
from pathlib import Path

from .chunk import encode_chunks_as_bytes
from .chunk import decode_chunks

REQUEST_RETRIES_COUNT = 5

def _create_session(url):
    """Create a session object to handle retries in case of server failure"""
    session = requests.Session()
    retries = Retry(total=REQUEST_RETRIES_COUNT, backoff_factor=1,
                    status_forcelist=[502, 503, 504])
    session.mount(url, HTTPAdapter(max_retries=retries))
    return session

class File:
    owner: str
    path: str
    name: str
    directory: bool
    size: int
    lastModifiedTime: str

    def __init__(self, owner: str, path: str, name: str, directory, size, lastModifiedTime):
        self.owner = owner
        self.path = path
        self.name = name
        self.directory = directory.lower() == 'true'
        self.size = int(size) if size != '' else 0
        self.lastModifiedTime = lastModifiedTime

    def __str__(self):
        return f'File[{self.owner},{self.path},{self.name},{self.directory},{self.size},{self.lastModifiedTime}]'


class AfsClient:

    def __init__(self, url, sessionToken, verify=True):
        self._afs_url = url
        self._sessionToken = sessionToken
        self._verify = verify
        self.session = _create_session(url)


    def is_session_valid(self):
        request = {
            "sessionToken": self._sessionToken,
            # "interactiveSessionKey": None,
            # "transactionManagerKey": None,
            "method": "isSessionValid",
        }
        try:
            with self.session.get(self._afs_url, params=request, verify=self._verify, stream=True) as r:
                content = r.content.decode("utf-8").lower() == "true"
                return content
        except BaseException:
            return False

    def list(self, owner, source, recursively=False):
        request = {
            "sessionToken": self._sessionToken,
            # "interactiveSessionKey": None,
            # "transactionManagerKey": None,
            "method": "list",
            "owner": owner,
            "source": source,
            "recursively": recursively
        }

        with self.session.get(self._afs_url, params=request, verify=self._verify, stream=True) as response:
            if response.ok:
                entries = response.text.split(';')
                result = []
                for entry in entries:
                    single_file = entry.split(",")
                    file = File(*single_file)
                    result += [file]
                return result
            else:
                parsed_error = json.loads(response.text)
                message = parsed_error['error'][1]['message']
                if "NoSuchFileException" in message:
                    return []
                raise ValueError(
                    f"Error {message['error'][1]['exceptionCode']} during list: {message['error'][1]['message']}"
                )

    def preview(self, owner, source):
        request = {
            "sessionToken": self._sessionToken,
            # "interactiveSessionKey": None,
            # "transactionManagerKey": None,
            "method": "preview",
            "owner": owner,
            "source": source,
        }

        with self.session.get(self._afs_url, params=request, verify=self._verify, stream=True) as r:
            return r.content


    def read(self, owner, source, offset, limit):
        params= {
            "sessionToken": self._sessionToken,
            # "interactiveSessionKey": None,
            # "transactionManagerKey": None,
            "method": "read",
        }
        chunks = [{
                "@type": "ch.ethz.sis.afsapi.dto.Chunk",
                "owner": owner,
                "source": source,
                "offset": offset,
                "limit": limit,
                "data": []
            }
        ]

        chunks_encoded = encode_chunks_as_bytes(chunks)

        with self.session.post(self._afs_url, data=chunks_encoded, params=params, verify=self._verify, stream=True) as r:
            content = r.content
            decoded = decode_chunks(content)[0]
            return decoded['data']

    def write(self, owner, source, offset, limit, data):
        params= {
            "sessionToken": self._sessionToken,
            # "interactiveSessionKey": None,
            # "transactionManagerKey": None,
            "method": "write",
        }
        chunks = [{
                "@type": "ch.ethz.sis.afsapi.dto.Chunk",
                "owner": owner,
                "source": source,
                "offset": offset,
                "limit": limit,
                "data": data
            }
        ]

        chunks_encoded = encode_chunks_as_bytes(chunks)

        with self.session.post(self._afs_url, data=chunks_encoded, params=params, verify=self._verify, stream=True) as r:
            content = r.content.decode("utf-8").lower() == "true"
            return content

    def create(self, owner, source, is_directory):
        params = {
            "sessionToken": self._sessionToken,
            # "interactiveSessionKey": None,
            # "transactionManagerKey": None,
            "method": "create",
            "owner": owner,
            "source": source,
            "directory": is_directory
        }

        with self.session.post(self._afs_url, data=params, params=params, verify=self._verify, stream=True) as r:
            content = r.content.decode("utf-8").lower() == "true"
            return content

    def upload_files(self, owner, source_path, files, wait_until_finished=True):
        file_list = self.list(owner, "/", True)
        existing_files = set()
        for file in file_list:
            existing_files.add(file.path)
        real_files = []
        for filename in files:
            if os.path.isdir(filename):
                pardir = os.path.join(filename, os.pardir)
                for root, dirs, path_files in os.walk(os.path.expanduser(filename)):
                    path = os.path.relpath(root, pardir)
                    for file in path_files:
                        real_files.append((path, os.path.join(root, file)))
                    if not path_files:
                        # append empty folder
                        real_files.append((path, ""))
            else:
                real_files.append(("", os.path.join(filename)))

        with AfsFileUploadQueue(self._afs_url) as queue:
            for filename in real_files:
                file_path_afs = os.path.join(source_path, filename[0], os.path.basename(filename[1]))
                if file_path_afs in existing_files:
                #     TODO delete flow?
                    pass

                queue.put((file_path_afs, filename[1], self._sessionToken, owner))

            # wait until all files have uploaded
            if wait_until_finished:
                try:
                    queue.join()
                except BaseException as e:
                    raise e

    def download_files(self, owner, source, destination, wait_until_finished=True):
        file_list = self.list(owner, source, True)
        if not os.path.exists(destination):
            os.makedirs(os.path.dirname(destination), exist_ok=True)

        with AfsFileDownloadQueue(self._afs_url) as queue:
            for file in file_list:
                if file.directory:
                    os.makedirs(os.path.join(destination, file.path[1:]), exist_ok=True)
                else:
                    queue.put((file, self._sessionToken, destination))

            # wait until all files have uploaded
            if wait_until_finished:
                try:
                    queue.join()
                except BaseException as e:
                    raise e




class PropagatingThread(Thread):
    def run(self):
        self.exc = None
        try:
            self.ret = self._target(*self._args, **self._kwargs)
        except BaseException as e:
            self.exc = e

    def join(self, timeout=None):
        super(PropagatingThread, self).join(timeout)
        if self.exc:
            raise self.exc
        return self.ret

class AfsFileUploadQueue:
    """Structure for uploading files to AFS in separate threads.
    It works as a queue where each item is a single file upload. """


    def __init__(self, url, workers=10):
        self.url = url
        self.session = _create_session(url)
        self.items = []
        # maximum files to be uploaded at once
        self.upload_queue = Queue()
        self.workers = workers
        self.threads = []
        self.exceptions = Queue()
        self.cancelled = threading.Event()
        self._drain_lock = threading.Lock()
        # define number of threads and start them
        for t in range(workers):
            t = PropagatingThread(target=self.upload_file)
            self.threads += [t]
            t.start()

    def __enter__(self, *args, **kwargs):
        return self

    def __exit__(self, *args, **kwargs):
        """This method is called at the end of a with statement."""
        # stop the workers
        for i in range(self.workers):
            self.upload_queue.put(None)
        # ensure clean shutdown
        for t in self.threads:
            t.join()

    def put(self, item):
        """expects a list [afs_path, local_file_path] which is put into the upload queue"""
        self.upload_queue.put(item)

    def join(self):
        # wait for all tasks (including those we mark done in the drainer)
        self.upload_queue.join()
        if not self.exceptions.empty():
            raise self.exceptions.get()
        for t in self.threads:
            if getattr(t, "exc", None):
                raise t.exc

    def upload_file(self):
        while True:
            # get the next item in the queue
            item = self.upload_queue.get()
            if item is None:
                # sentinel from __exit__
                self.upload_queue.task_done()
                break

            # if another worker already failed, drop this task quickly
            if self.cancelled.is_set():
                self.upload_queue.task_done()
                continue

            (afs_path, file_path, session_token, owner) = item

            try:
                if file_path == '':
                    params = {
                        "sessionToken": session_token,
                        # "interactiveSessionKey": None,
                        # "transactionManagerKey": None,
                        "method": "create",
                        "owner": owner,
                        "source": afs_path,
                        "directory": True
                    }

                    response = self.session.post(self.url, data=params, params=params, stream=True)
                    if response.ok:
                        content = response.content.decode("utf-8").lower() == "true"
                        if not content:
                            message = json.loads(response.text)
                            raise ValueError(
                                f"Error {message['error'][1]['exceptionCode']} during upload: {message['error'][1]['message']}"
                            )
                    response.raise_for_status()
                else:
                    file_size = os.path.getsize(file_path)
                    size = 1024 * 1024 * 10  # 10MB

                    params = {
                        "sessionToken": session_token,
                        # "interactiveSessionKey": None,
                        # "transactionManagerKey": None,
                        "method": "write",
                    }
                    if file_size > size:
                        with open(file_path, "rb") as f:
                            for i in range(0, file_size, size):
                                range_to_get = file_size - i if i + size > file_size else size
                                data = f.read(range_to_get)
                                chunks = [{
                                    # "owner": 'aaaa',
                                    "owner": owner,
                                    "source": afs_path,
                                    "offset": i,
                                    "limit": i + range_to_get,
                                    "data": data
                                }]

                                chunks_encoded = encode_chunks_as_bytes(chunks)

                                with self.session.post(self.url, data=chunks_encoded, params=params, stream=True) as response:
                                    if response.ok:
                                        content = response.content.decode("utf-8").lower() == "true"
                                        if not content:
                                            message = json.loads(response.text)
                                            raise ValueError(
                                                        f"Error {message['error'][1]['exceptionCode']} during upload: {message['error'][1]['message']}"
                                                    )
                                    response.raise_for_status()
                    else:
                        with open(file_path, "rb") as f:

                            chunks = [{
                                "owner": owner,
                                "source": afs_path,
                                "offset": 0,
                                "limit": file_size,
                                "data": f.read()
                            }]

                            chunks_encoded = encode_chunks_as_bytes(chunks)

                            with self.session.post(self.url, data=chunks_encoded, params=params, stream=True) as response:
                                content = response.content.decode("utf-8").lower() == "true"
                                if not content:
                                    message = json.loads(response.text)
                                    raise ValueError(
                                        f"Error {message['error'][1]['exceptionCode']} during upload: {message['error'][1]['message']}"
                                    )
                                response.raise_for_status()

            except BaseException as e:
                # make sure only the *first* failing worker drains the queue
                first = False
                with self._drain_lock:
                    if not self.cancelled.is_set():
                        self.cancelled.set()
                        first = True
                        if self.exceptions.empty():
                            self.exceptions.put(e)

                if first:
                    # drain remaining tasks so queue.join() can finish
                    while True:
                        try:
                            leftover = self.upload_queue.get_nowait()
                        except Empty:
                            break
                        else:
                            # mark each drained item done (we didn't process them)
                            self.upload_queue.task_done()

                # mark the *current* item done exactly once and exit this worker
                self.upload_queue.task_done()
                return
            else:
                # normal success path
                self.upload_queue.task_done()

class AfsFileDownloadQueue:
    def __init__(self, url, workers=10):
        self.url = url
        self.session = _create_session(url)
        self.items = []
        # maximum files to be downloaded at once
        self.download_queue = Queue()
        self.workers = workers
        self.threads = []
        self.exceptions = Queue()
        self.cancelled = threading.Event()
        self._drain_lock = threading.Lock()
        # define number of threads and start them
        for t in range(workers):
            t = PropagatingThread(target=self.download_file)
            self.threads += [t]
            t.start()

    def __enter__(self, *args, **kwargs):
        return self

    def __exit__(self, *args, **kwargs):
        """This method is called at the end of a with statement."""
        # stop the workers
        for i in range(self.workers):
            self.download_queue.put(None)
        # ensure clean shutdown
        for t in self.threads:
            t.join()

    def put(self, item):
        """expects a tuple (file, session_token, destination) which is put into the download queue"""
        self.download_queue.put(item)

    def join(self):
        # wait for all tasks (including those we mark done in the drainer)
        self.download_queue.join()
        if not self.exceptions.empty():
            raise self.exceptions.get()
        for t in self.threads:
            if getattr(t, "exc", None):
                raise t.exc

    def download_file(self):
        while True:
            # get the next item in the queue
            item = self.download_queue.get()
            if item is None:
                # sentinel from __exit__
                self.download_queue.task_done()
                break

            # if another worker already failed, drop this task quickly
            if self.cancelled.is_set():
                self.download_queue.task_done()
                continue

            (file, session_token, destination) = item

            try:
                file_size = file.size
                size = 1024 * 1024 * 10  # 10MB

                # os.path.join(destination, file.path)
                file_dest = os.path.join(destination, file.path[1:])
                if not os.path.exists(file_dest):
                    Path(file_dest).touch(exist_ok=True)

                for i in range(0, file_size, size):
                    range_to_get = file_size - i if i + size > file_size else size

                    params = {
                        "sessionToken": session_token,
                        # "interactiveSessionKey": None,
                        # "transactionManagerKey": None,
                        "method": "read",
                    }
                    chunks = [{
                        "@type": "ch.ethz.sis.afsapi.dto.Chunk",
                        "owner": file.owner,
                        "source": file.path,
                        "offset": i,
                        "limit": range_to_get,
                        "data": []
                    }]

                    chunks_encoded = encode_chunks_as_bytes(chunks)

                    with self.session.post(self.url, data=chunks_encoded, params=params, stream=True) as r:
                        content = r.content
                        decoded = decode_chunks(content)[0]
                        data = decoded['data']

                        with open(file_dest, "rb+") as local_file:
                            local_file.seek(i)
                            local_file.write(data)





            except BaseException as e:
                # make sure only the *first* failing worker drains the queue
                first = False
                with self._drain_lock:
                    if not self.cancelled.is_set():
                        self.cancelled.set()
                        first = True
                        if self.exceptions.empty():
                            self.exceptions.put(e)

                if first:
                    # drain remaining tasks so queue.join() can finish
                    while True:
                        try:
                            leftover = self.download_queue.get_nowait()
                        except Empty:
                            break
                        else:
                            # mark each drained item done (we didn't process them)
                            self.download_queue.task_done()

                # mark the *current* item done exactly once and exit this worker
                self.download_queue.task_done()
                return
            else:
                # normal success path
                self.download_queue.task_done()
