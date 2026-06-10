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
"""DataSet entity and file-transfer helpers for openBIS.

A :class:`DataSet` stores the actual files associated with scientific
experiments.  There are three kinds:

- ``"PHYSICAL"`` — stores files in the openBIS data store server (DSS).
- ``"CONTAINER"`` — groups other datasets; holds no files itself.
- ``"LINK"`` — references files in an external data-management system.

Key helpers:
    - :class:`DataSetUploadQueueNew` — multi-threaded V3 upload queue.
    - :class:`DataSetUploadQueue` — multi-threaded V1 upload queue.
    - :class:`DataSetDownloadQueue` — multi-threaded V1 download queue.
    - :class:`ZipBuffer` — streaming zip upload buffer (V1 only).
    - :class:`PhysicalData` — physical storage metadata of a dataset.
    - :class:`LinkedData` — linked-data metadata of a dataset.
"""

import json
import os
import random
import time
import urllib.parse
import uuid
import zipfile
from functools import partialmethod
from pathlib import Path
from queue import Queue, Empty
import threading
from threading import Thread
from typing import Any, Optional, Set, List, TYPE_CHECKING
from urllib.parse import urljoin, quote

import requests
import copy
from pandas import DataFrame
from requests import Session
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from tabulate import tabulate

from .definitions import (
    openbis_definitions,
    get_type_for_entity,
    get_fetchoption_for_entity,
)
from .fast_download import FastDownload
from .openbis_object import OpenBisObject
from .openbis_typing import DataSetKind, PermId
from .things import Things
from .utils import (
    VERBOSE,
    parse_jackson,
    extract_permid,
    extract_code,
    extract_downloadUrl,
)

if TYPE_CHECKING:
    from .pybis import Openbis

PYBIS_PLUGIN = "dataset-uploader-api"
DATASET_DEFINITIONS = openbis_definitions("dataSet")
DSS_ENDPOINT = "/datastore_server/rmi-data-store-server-v3.json"
SESSION_WORKSPACE = "/datastore_server/session_workspace_file_upload"
REQUEST_RETRIES_COUNT = 5


def signed_to_unsigned(sig_int: int) -> str:
    """Convert a signed CRC32 integer to an unsigned hex string.

    openBIS delivers CRC32 checksums as signed 32-bit integers.  If the
    value is negative we add 2³² to get the unsigned equivalent, then
    format it as a hex string to match the classic UI display.

    Args:
        sig_int: Signed CRC32 integer.

    Returns:
        Unsigned hex string, e.g. ``"deadbeef"``.
    """
    if sig_int < 0:
        sig_int += 2**32
    return "%x" % (sig_int & 0xFFFFFFFF)


class DataSet(
    OpenBisObject,
    entity="dataSet",
    single_item_method_name="get_dataset",
):
    """An openBIS dataset — the primary container for scientific files.

    Datasets link files in the data store server (DSS) to experiments or
    samples and carry typed metadata properties.  They have three kinds:

    - ``"PHYSICAL"`` — requires at least one file on creation.
    - ``"CONTAINER"`` — groups datasets; no files allowed.
    - ``"LINK"`` — references an external data management system.

    Fetch datasets via ``openbis.get_dataset()`` /
    ``openbis.get_datasets()`` and create new ones with
    ``openbis.new_dataset()``.

    **Properties** are accessed via ``dataset.props``::

        ds.props.description = "Raw sequencing output"
        ds.save()

    **File access** (physical datasets only)::

        ds.file_list  # list of file paths
        ds.download(destination="./data")
        ds.file_links  # dict of absolute download URLs

    Attributes:
        permId (str): Server-assigned permanent identifier.
        code (str): Dataset code (same as ``permId``).
        type (Any): The :class:`~pybis.entity_type.DataSetType` of this dataset.
        props (PropertyHolder): :class:`~pybis.property.PropertyHolder` for typed properties.
        physicalData (:class:`PhysicalData`): Physical storage metadata
            (location, size, archive status, …).
        linkedData (:class:`LinkedData`): External link metadata.
        parents (list): Identifiers of parent datasets.
        children (list): Identifiers of child datasets.
        container (list): Identifiers of container datasets.
        component (list): Identifiers of component datasets.

    Example:
        >>> ds = openbis.new_dataset(
        ...     type="RAW_DATA",
        ...     experiment="/MY_SPACE/MY_PROJECT/EXP_001",
        ...     files=["data.csv"],
        ... )
        >>> ds.props.description = "Measurement results"
        >>> ds.save()
        >>> ds.file_list
        ['data.csv']
    """

    def __init__(
        self,
        openbis_obj: Openbis,
        type: Any,
        data: Optional[dict] = None,
        files: Optional[Any] = None,
        zipfile: Optional[str] = None,
        folder: Optional[str] = None,
        kind: Optional[DataSetKind] = None,
        props: Optional[dict] = None,
        **kwargs: Any,
    ) -> None:
        """Initialise a DataSet instance.

        Args:
            openbis_obj: The :class:`~pybis.Openbis` connection instance.
            type: The dataset type object.
            data: Raw dataset dict from the V3 API.  When provided the
                attributes and properties are populated automatically.
            files: A single file path or list of file paths to upload
                (``kind="PHYSICAL"`` only).
            zipfile: Path to a single zip file whose contents are extracted
                on the server side.  Mutually exclusive with ``files``.
            folder: Optional folder name used during upload (V1 only).
            kind: Dataset kind — ``"PHYSICAL"``, ``"CONTAINER"``, or
                ``"LINK"``.  Inferred from the type if not given.
            props: Initial property values as a ``{code: value}`` dict.
            **kwargs: Additional attributes (e.g. ``experiment``, ``sample``,
                ``parents``).

        Raises:
            ValueError: If ``kind="PHYSICAL"`` and neither ``files`` nor
                ``zipfile`` is provided, or if both are provided.
            ValueError: If any file in ``files`` does not exist on disk.
            ValueError: If ``kind`` is not one of the allowed values.
        """
        if kind == "PHYSICAL":
            if files is None and zipfile is None:
                raise ValueError("please provide at least one file")

            if files and zipfile:
                raise ValueError(
                    "please provide either a list of files or a single zipfile"
                )

            if zipfile:
                files = [zipfile]
                self.__dict__["isZipDirectoryUpload"] = True
            else:
                self.__dict__["isZipDirectoryUpload"] = False

            if files:
                if isinstance(files, str):
                    files = [files]

                for file in files:
                    if not os.path.exists(file):
                        raise ValueError(f"File {file} does not exist")

                self.__dict__["files"] = files

        super().__init__(openbis_obj, type=type, data=data, props=props, **kwargs)

        self.__dict__["files_in_wsp"] = []

        if data is not None:
            if data["physicalData"] is None:
                self.__dict__["shareId"] = None
                self.__dict__["location"] = None
            else:
                self.__dict__["shareId"] = data["physicalData"]["shareId"]
                self.__dict__["location"] = data["physicalData"]["location"]

        if kind is not None:
            kind = kind.upper()
            allowed_kinds = ["PHYSICAL", "CONTAINER", "LINK"]
            if kind not in allowed_kinds:
                raise ValueError(
                    f"only these values are allowed for kind: {allowed_kinds}"
                )
            self.a.__dict__["_kind"] = kind

        self.__dict__["folder"] = folder

        if getattr(self, "parents") is None:
            self.a.__dict__["_parents"] = []
        else:
            if not self.is_new:
                self.a.__dict__["_parents_orig"] = copy.copy(
                    self.a.__dict__["_parents"]
                )

        if getattr(self, "children") is None:
            self.a.__dict__["_children"] = []
        else:
            if not self.is_new:
                self.a.__dict__["_children_orig"] = copy.copy(
                    self.a.__dict__["_children"]
                )

        if getattr(self, "container") is None:
            self.a.__dict__["_container"] = []
        else:
            if not self.is_new:
                self.a.__dict__["_container_orig"] = self.a.__dict__["_container"]

        if getattr(self, "component") is None:
            self.a.__dict__["_component"] = []
        else:
            if not self.is_new:
                self.a.__dict__["_component_orig"] = self.a.__dict__["_component"]

    def __str__(self) -> str:
        """String representation of this dataset returns its code."""
        return self.data["code"]

    def __dir__(self) -> list[str]:
        """Return public attributes and methods for tab-completion.

        Returns:
            A list of attribute and method names available on this dataset.
        """
        return [
            "get_parents()",
            "get_children()",
            "get_components()",
            "get_contained()",
            "get_containers()",
            "add_parents()",
            "add_children()",
            "add_components()",
            "add_contained()",
            "add_containers()",
            "del_parents()",
            "del_children()",
            "del_components()",
            "del_contained()",
            "del_containers()",
            "set_parents()",
            "set_children()",
            "set_components()",
            "set_contained()",
            "set_containers()",
            "set_tags()",
            "add_tags()",
            "del_tags()",
            "add_attachment()",
            "get_attachments()",
            "download_attachments()",
            "get_files()",
            "file_list",
            "file_links",
            "rel_file_links",
            "physicalData",
            "download()",
            "download_path",
            "is_physical()",
            "symlink()",
            "is_symlink()",
            "archive()",
            "unarchive()",
            "save()",
            "delete()",
            "mark_to_be_deleted()",
            "unmark_to_be_deleted()",
            "is_marked_to_be_deleted()",
            "attrs",
            "props",
        ] + super().__dir__()

    def __setattr__(self, name: str, value: Any) -> None:
        """Custom attribute setting to handle special cases for certain fields.

        Args:
            name: Name of the attribute being set.
                e.g., 'folder', 'p', 'props', or any other attribute.
            value: Value to set for the attribute.
        """
        if name in ["folder"]:
            self.__dict__[name] = value
        elif name in ["p", "props"]:
            if isinstance(value, dict):
                for p in value:
                    setattr(self.__dict__["p"], p, value[p])
            else:
                raise ValueError("please provide a dictionary for setting properties")
        else:
            super(DataSet, self).__setattr__(name, value)

    def get_eln_url(self) -> str:
        """Return the direct URL to this dataset in the ELN-LIMS web UI.

        Returns:
            A URL string that opens the dataset page in a browser.

        Example:
            >>> print(ds.get_eln_url())
            https://openbis.example.com/webapp/eln-lims/?menuUniqueId=...
        """
        query = {"type": "DATASET", "id": self.permId}
        return (
            f"{self.openbis.url}/webapp/eln-lims/?menuUniqueId={quote(str(query))}&"
            f"viewName=showViewDataSetPageFromPermId&viewData={self.permId}"
        )

    @property
    def props(self) -> Any:
        """The :class:`~pybis.property.PropertyHolder` for this dataset's properties."""
        return self.__dict__["p"]

    @property
    def type(self) -> Any:
        """The :class:`~pybis.entity_type.DataSetType` of this dataset."""
        return self.__dict__["type"]

    @type.setter
    def type(self, type_name: str) -> None:
        """Change the dataset type.

        Args:
            type_name: Code of the new dataset type (upper-cased automatically).
        """
        dataset_type = self.openbis.get_dataset_type(type_name.upper())
        self.p.__dict__["_type"] = dataset_type
        self.a.__dict__["_type"] = dataset_type

    @property
    def physicalData(self) -> Optional["PhysicalData"]:
        """Physical storage metadata for this dataset.

        Returns:
            A :class:`PhysicalData` object, or ``None`` for non-physical
            datasets.
        """
        if "physicalData" in self.data:
            return PhysicalData(data=self.data["physicalData"])

    @property
    def linkedData(self) -> Optional["LinkedData"]:
        """Linked-data metadata for this dataset.

        Returns:
            A :class:`LinkedData` object, or ``None`` for non-link datasets.
        """
        if "linkedData" in self.data:
            return LinkedData(data=self.data["linkedData"])

    @property
    def status(self) -> Optional[str]:
        """Current archive status of this dataset.

        Fetches fresh data from the server on every access.

        Returns:
            Status string (e.g. ``"AVAILABLE"``, ``"ARCHIVED"``), or ``None``
            if not a physical dataset.
        """
        ds = self.openbis.get_dataset(self.permId)
        self.data["physicalData"] = ds.data["physicalData"]
        try:
            return self.data["physicalData"]["status"]
        except Exception:
            return None

    @property
    def download_path(self) -> str:
        """Local path where files were downloaded.

        Set by :meth:`download` and :meth:`_download_physical` after a
        successful download.

        Returns:
            Relative path string, or ``""`` if no download has occurred.
        """
        return self.__dict__.get("download_path", "")

    @property
    def _sftp_source_dir(self) -> str:
        """SFTP path for this dataset under the mountpoint.

        Constructed as ``SPACE/PROJECT/EXPERIMENT/permId`` (without leading
        slash).

        Returns:
            Relative SFTP source directory string.
        """
        return os.path.join(self.experiment.identifier[1:], self.permId)

    def symlink(
        self,
        target_dir: Optional[str] = None,
        replace_if_symlink_exists: bool = True,
    ) -> str:
        """Create a local symlink pointing to this dataset's SFTP directory.

        The openBIS instance must be SFTP-mountable.  The symlink target is
        the dataset's directory inside the SFTP mountpoint.

        Args:
            target_dir: Local path for the symlink.  Defaults to
                ``<download_prefix>/<permId>``.
            replace_if_symlink_exists: If ``True`` and ``target_dir`` is
                already a symlink, the old symlink is removed before creating
                the new one.

        Returns:
            Absolute path of the created symlink.

        Raises:
            ValueError: If the openBIS instance cannot be mounted, or if the
                SFTP source path does not exist.
        """
        if target_dir is None:
            target_dir = os.path.join(self.openbis.download_prefix, self.permId)

        target_dir_path = Path(target_dir)
        if target_dir_path.is_symlink() and replace_if_symlink_exists:
            target_dir_path.unlink()

        os.makedirs(os.path.dirname(target_dir_path.absolute()), exist_ok=True)

        mountpoint_path = self.openbis.get_mountpoint()
        if mountpoint_path is None:
            try:
                mountpoint_path = self.openbis.mount()
            except ValueError as err:
                if "password" in str(err):
                    raise ValueError(
                        "openBIS instance cannot be mounted, no symlink possible"
                    )

        sftp_source_path = os.path.join(mountpoint_path, self._sftp_source_dir)

        if os.path.exists(sftp_source_path):
            target_dir_path.symlink_to(sftp_source_path, target_is_directory=True)
            if VERBOSE:
                print(f"Symlink created: {target_dir} --> {sftp_source_path}")

            return str(target_dir_path.absolute())
        else:
            raise ValueError(
                f"Source path {sftp_source_path} does not exist, cannot create symlink"
            )

    @staticmethod
    def _file_set(target_dir: str) -> Set[str]:
        """Return the set of relative file paths inside a directory.

        Args:
            target_dir: Root directory to scan recursively.

        Returns:
            Set of relative path strings for every file found.
        """
        target_dir_path = Path(target_dir)
        return set(
            str(el.relative_to(target_dir_path))
            for el in target_dir_path.glob("**/*")
            if el.is_file()
        )

    def _is_symlink_or_physical(
        self,
        what: str,
        target_dir: Optional[str] = None,
        expected_file_list: Optional[List[str]] = None,
    ) -> bool:
        """Check whether the local download directory is a symlink or physical copy.

        Verifies that all expected files are present under ``target_dir``,
        then checks whether it is a symlink (for ``what="symlink"``) or a
        regular directory (for ``what="physical"``).

        Args:
            what: ``"symlink"`` or ``"physical"``.
            target_dir: Local directory to check.  Defaults to
                ``<download_prefix>/<permId>``.
            expected_file_list: Override the file list used for the subset
                check.  If ``None``, :attr:`file_list` is used.

        Returns:
            ``True`` if the directory exists and all expected files are
            present with the requested link type.

        Raises:
            ValueError: If ``what`` is neither ``"symlink"`` nor
                ``"physical"``.
        """
        if target_dir is None:
            target_dir = os.path.join(self.openbis.download_prefix, self.permId)
        target_dir_path = Path(target_dir)

        target_file_set = self._file_set(target_dir)

        if expected_file_list is None:
            source_file_set = set(self.file_list)
        else:
            source_file_set = set(expected_file_list)

        res = source_file_set.issubset(target_file_set)
        if not res:
            return res
        elif what == "symlink":
            return target_dir_path.exists() and target_dir_path.is_symlink()
        elif what == "physical":
            return target_dir_path.exists() and not target_dir_path.is_symlink()
        else:
            raise ValueError("Unexpected error")

    is_symlink = partialmethod(
        _is_symlink_or_physical, what="symlink", expected_file_list=None
    )
    """Check whether the local copy is a symlink (``is_symlink(target_dir=None)``)."""

    is_physical = partialmethod(_is_symlink_or_physical, what="physical")
    """Check whether the local copy is a physical directory (``is_physical(target_dir=None)``)."""

    def archive(self, remove_from_data_store: bool = True) -> None:
        """Archive this dataset, optionally removing it from the data store.

        Args:
            remove_from_data_store: If ``True`` (default) the files are
                removed from the hot data store after archiving.

        Example:
            >>> ds.archive()
            >>> ds.archive(remove_from_data_store=False)
        """
        fetchopts = {
            "removeFromDataStore": remove_from_data_store,
            "@type": "as.dto.dataset.archive.DataSetArchiveOptions",
        }
        self.archive_unarchive("archiveDataSets", fetchopts)
        if VERBOSE:
            print(f"DataSet {self.permId} archived")

    def unarchive(self) -> None:
        """Unarchive this dataset, restoring it to the hot data store.

        Example:
            >>> ds.unarchive()
        """
        fetchopts = {"@type": "as.dto.dataset.unarchive.DataSetUnarchiveOptions"}
        self.archive_unarchive("unarchiveDataSets", fetchopts)
        if VERBOSE:
            print(f"DataSet {self.permId} unarchived")

    def archive_unarchive(self, method: str, fetchopts: dict) -> None:
        """Send an archive or unarchive request to the V3 API.

        Args:
            method: V3 API method name (``"archiveDataSets"`` or
                ``"unarchiveDataSets"``).
            fetchopts: Options dict for the request.
        """
        payload = {}

        request = {
            "method": method,
            "params": [
                self.openbis.token,
                [{"permId": self.permId, "@type": "as.dto.dataset.id.DataSetPermId"}],
                dict(fetchopts),
            ],
        }
        resp = self.openbis._post_request(self._openbis.as_v3, request)
        return

    def set_properties(self, properties: dict) -> None:
        """Set multiple properties at once from a dictionary.

        Does not save the dataset — call :meth:`save` afterwards.

        Args:
            properties: A ``{property_code: value}`` dictionary.

        Example:
            >>> ds.set_properties({"description": "Raw data", "quality": "HIGH"})
            >>> ds.save()
        """
        for prop in properties.keys():
            setattr(self.p, prop, properties[prop])

    set_props = set_properties

    def get_dataset_files(
        self,
        start_with: Optional[int] = None,
        count: Optional[int] = None,
        **properties: Any,
    ) -> Things:
        """Return a :class:`~pybis.things.Things` container of dataset file records.

        Each record includes the file path, directory flag, file size,
        CRC32 checksum, and download URL.  This is the underlying call used
        by :attr:`file_list`, :meth:`get_files`, and :meth:`download`.

        Args:
            start_with: Pagination offset.
            count: Maximum number of records to return.
            **properties: Unused — reserved for future filtering.

        Returns:
            A :class:`~pybis.things.Things` container whose ``.df`` gives a
            :class:`~pandas.DataFrame` with columns ``dataSetPermId``,
            ``dataStore``, ``downloadUrl``, ``path``, ``directory``,
            ``fileLength``, ``checksumCRC32``, ``checksum``,
            ``checksumType``.
        """
        search_criteria = get_type_for_entity("dataSetFile", "search")
        search_criteria["operator"] = "AND"
        search_criteria["criteria"] = [
            {
                "criteria": [
                    {
                        "fieldName": "code",
                        "fieldType": "ATTRIBUTE",
                        "fieldValue": {
                            "value": self.permId,
                            "@type": "as.dto.common.search.StringEqualToValue",
                        },
                        "@type": "as.dto.common.search.CodeSearchCriteria",
                    }
                ],
                "operator": "OR",
                "@type": "as.dto.dataset.search.DataSetSearchCriteria",
            }
        ]

        fetchopts = get_fetchoption_for_entity("dataSetFile")

        request = {
            "method": "searchFiles",
            "params": [
                self.openbis.token,
                search_criteria,
                fetchopts,
            ],
        }
        full_url = urljoin(self._get_download_url(), DSS_ENDPOINT)
        resp = self.openbis._post_request_full_url(full_url, request)

        def create_data_frame(attrs: Any, props: Any, response: list) -> DataFrame:
            objects = response["objects"]
            parse_jackson(objects)

            attrs = [
                "dataSetPermId",
                "dataStore",
                "downloadUrl",
                "path",
                "directory",
                "fileLength",
                "checksumCRC32",
                "checksum",
                "checksumType",
            ]

            dataSetFiles = None
            if len(objects) == 0:
                dataSetFiles = DataFrame(columns=attrs)
            else:
                dataSetFiles = DataFrame(objects)
                dataSetFiles["downloadUrl"] = dataSetFiles["dataStore"].map(
                    extract_downloadUrl
                )
                dataSetFiles["checksumCRC32"] = (
                    dataSetFiles["checksumCRC32"]
                    .fillna(0.0)
                    .astype(int)
                    .map(signed_to_unsigned)
                )
                dataSetFiles["dataStore"] = dataSetFiles["dataStore"].map(extract_code)
                dataSetFiles["dataSetPermId"] = dataSetFiles["dataSetPermId"].map(
                    extract_permid
                )
            return dataSetFiles[attrs]

        return Things(
            openbis_obj=self.openbis,
            entity="dataSetFile",
            identifier_name="dataSetPermId",
            start_with=start_with,
            count=count,
            totalCount=resp.get("totalCount"),
            response=resp,
            df_initializer=create_data_frame,
        )

    def download(
        self,
        files: Optional[Any] = None,
        destination: Optional[str] = None,
        create_default_folders: bool = True,
        wait_until_finished: bool = True,
        workers: int = 10,
        linked_dataset_fileservice_url: Optional[str] = None,
        content_copy_index: int = 0,
    ) -> Any:
        """Download files from this dataset to a local directory.

        Dispatches to the appropriate download implementation based on the
        dataset kind (``PHYSICAL`` → fast download if server ≥ 3.5, otherwise
        V1; ``LINK`` → microservice download).

        Args:
            files: A single file path string or list of paths relative to
                the dataset root.  If ``None``, all files are downloaded.
            destination: Local base directory.  Defaults to
                ``openbis.download_prefix``.  Files land under
                ``destination/<permId>/``.
            create_default_folders: If ``True`` (default), recreates the
                ``original/DEFAULT/`` folder structure from openBIS.  Set to
                ``False`` to flatten the layout — combine with a custom
                ``destination`` to control placement precisely.
            wait_until_finished: If ``True`` (default), block until all
                downloads complete.  Set to ``False`` to return immediately
                and let downloads proceed in the background.
            workers: Number of parallel download threads (default 10, V1
                only).
            linked_dataset_fileservice_url: Required for ``LINK`` datasets —
                the base URL of the microservice that provides file access.
            content_copy_index: Index into the ``contentCopies`` list for
                ``LINK`` datasets (default 0).

        Returns:
            For ``PHYSICAL`` datasets: the local destination path string.
            For ``LINK`` datasets: a ``(destination, files_with_wrong_length)``
            tuple.

        Raises:
            ValueError: For ``LINK`` datasets when
                ``linked_dataset_fileservice_url`` is not provided, or for
                unknown dataset kinds.

        Example:
            >>> ds.download(destination="./results")
            >>> ds.download(files=["original/DEFAULT/data.csv"])
        """
        if files == None:
            files = self.file_list
        elif isinstance(files, str):
            files = [files]

        if destination is None:
            destination = self.openbis.download_prefix

        kind = None
        if "kind" in self.data:
            kind = self.data["kind"]
        elif ("type" in self.data) and ("kind" in self.data["type"]):
            kind = self.data["type"]["kind"]

        if kind in ["PHYSICAL", "CONTAINER"]:
            if self.openbis.get_server_information().is_version_greater_than(3, 5):
                return self._download_fast_physical(
                    files, destination, create_default_folders, wait_until_finished
                )
            else:
                return self._download_physical(
                    files,
                    destination,
                    create_default_folders,
                    wait_until_finished,
                    workers,
                )
        elif kind == "LINK":
            if linked_dataset_fileservice_url is None:
                raise ValueError(
                    "Can't download a LINK data set without the linked_dataset_fileservice_url parameters."
                )
            return self._download_link(
                files,
                destination,
                wait_until_finished,
                workers,
                linked_dataset_fileservice_url,
                content_copy_index,
            )
        else:
            raise ValueError(f"Can't download data set of kind {kind}.")

    def _download_fast_physical(
        self,
        files: list,
        destination: str,
        create_default_folders: bool,
        wait_until_finished: bool,
    ) -> str:
        """Download using the fast V3 download scheme (server ≥ 3.5).

        Args:
            files: List of file paths to download.
            destination: Local base directory.
            create_default_folders: Whether to recreate the openBIS folder
                hierarchy under ``destination/<permId>/``.
            wait_until_finished: Block until download completes.

        Returns:
            The local destination directory path.
        """
        if create_default_folders:
            final_destination = os.path.join(destination, self.permId)
        else:
            final_destination = destination

        self.__dict__["download_path"] = final_destination

        download_url = self._get_download_url()

        fast_download = FastDownload(
            self.openbis.token,
            download_url,
            self.permId,
            files,
            final_destination,
            create_default_folders,
            wait_until_finished,
            self.openbis.verify_certificates,
            self.openbis.get_server_information(),
            wished_number_of_streams=4,
        )
        return fast_download.download()

    def _download_physical(
        self,
        files: list,
        destination: str,
        create_default_folders: bool,
        wait_until_finished: bool,
        workers: int,
    ) -> str:
        """Download using the legacy V1 API (server < 3.5).

        Args:
            files: List of file paths to download.
            destination: Local base directory.
            create_default_folders: Whether to recreate the openBIS folder
                hierarchy.
            wait_until_finished: Block until all downloads complete.
            workers: Number of parallel download threads.

        Returns:
            The local destination directory path.
        """
        final_destination = ""
        if create_default_folders:
            final_destination = os.path.join(destination, self.permId)
        else:
            final_destination = destination

        self.__dict__["download_path"] = final_destination

        download_url = self._get_download_url()
        base_url = download_url + "/datastore_server/" + self.permId + "/"
        with DataSetDownloadQueue(workers=workers) as queue:
            for filename in files:
                fi_df = self.get_dataset_files().df
                file_size = fi_df[fi_df["path"] == filename]["fileLength"].values[0]
                download_url = base_url + filename + "?sessionID=" + self.openbis.token
                download_url = quote(download_url, safe=":/?=")
                filename_dest = ""
                if create_default_folders:
                    filename_dest = os.path.join(final_destination, filename)
                else:
                    if filename.startswith("original/"):
                        filename = filename.replace("original/", "", 1)
                    if filename.startswith("DEFAULT/"):
                        filename = filename.replace("DEFAULT/", "", 1)
                    filename_dest = os.path.join(final_destination, filename)

                queue.put(
                    [
                        download_url,
                        filename,
                        filename_dest,
                        file_size,
                        self.openbis.verify_certificates,
                        "wb",
                    ]
                )

            if wait_until_finished:
                queue.join()

            if VERBOSE:
                print(f"Files downloaded to: {os.path.join(final_destination)}")
            return final_destination

    def _download_link(
        self,
        files: list[str],
        destination: str,
        wait_until_finished: bool,
        workers: int,
        linked_dataset_fileservice_url: str,
        content_copy_index: int,
    ) -> tuple:
        """Download from a LINK dataset via the microservice.

        Supports resumable downloads: if a partial file exists and its size
        is smaller than expected, an ``offset`` parameter is added to resume
        from where the previous download stopped.

        Args:
            files: List of file paths relative to the dataset root.
            destination: Local base directory.
            wait_until_finished: Block until all downloads complete.
            workers: Number of parallel download threads.
            linked_dataset_fileservice_url: Microservice base URL.
            content_copy_index: Index into ``contentCopies`` list.

        Returns:
            ``(destination, files_with_wrong_length)`` tuple — the second
            element lists files whose downloaded size did not match the
            expected size.

        Raises:
            ValueError: If ``content_copy_index`` is out of range.
        """
        with DataSetDownloadQueue(
            workers=workers, collect_files_with_wrong_length=True
        ) as queue:
            if content_copy_index >= len(self.data["linkedData"]["contentCopies"]):
                raise ValueError("Content Copy index out of range.")
            content_copy = self.data["linkedData"]["contentCopies"][content_copy_index]

            for filename in files:
                fi_df = self.get_dataset_files().df
                file_size = fi_df[fi_df["path"] == filename]["fileLength"].values[0]

                download_url = linked_dataset_fileservice_url
                download_url += "?sessionToken=" + self.openbis.token
                download_url += "&datasetPermId=" + self.data["permId"]["permId"]
                download_url += (
                    "&externalDMSCode=" + content_copy["externalDms"]["code"]
                )
                download_url += "&contentCopyPath=" + content_copy["path"].replace(
                    "/", "%2F"
                )
                download_url += "&datasetPathToFile=" + urllib.parse.quote(filename)

                filename_dest = os.path.join(destination, self.permId, filename)

                write_mode = "wb"
                if os.path.exists(filename_dest):
                    actual_size = os.path.getsize(filename_dest)
                    if actual_size == int(file_size):
                        continue
                    elif actual_size < int(file_size):
                        write_mode = "ab"
                        download_url += "&offset=" + str(actual_size)

                queue.put(
                    [
                        download_url,
                        filename,
                        filename_dest,
                        file_size,
                        self.openbis.verify_certificates,
                        write_mode,
                    ]
                )

            if wait_until_finished:
                queue.join()

            if VERBOSE:
                print(
                    "Files downloaded to: %s" % os.path.join(destination, self.permId)
                )
            return destination, queue.files_with_wrong_length

    @property
    def folder(self) -> Optional[str]:
        """Upload folder name (used during V1 upload).

        Returns:
            The folder string or ``None``.
        """
        return self.__dict__["folder"]

    @property
    def file_list(self) -> list[str]:
        """List of file paths contained in this dataset.

        For new (unsaved) datasets, returns the local file paths provided
        at creation time.  For existing datasets, queries the DSS for the
        current file list, excluding directory entries.

        Returns:
            A list of relative file path strings.

        Example:
            >>> ds.file_list
            >>> # ['original/DEFAULT/data.csv', 'original/DEFAULT/meta.json']
        """
        if self.is_new:
            return self.files
        else:
            fl = self.get_dataset_files().df
            return fl[fl["directory"] == False]["path"].to_list()

    @property
    def file_links(self) -> dict:
        """Absolute download URLs for all files in this dataset.

        Each URL includes the session token (``sessionID``), so sharing links
        is a security risk.  Links become invalid when the token expires.

        Returns:
            A ``{relative_path: url}`` dict, or ``""`` for new datasets.

        Example:
            >>> for path, url in ds.file_links.items():
            ...     print(path, url)
            original/DEFAULT/data.csv https://openbis.example.com/datastore_server/...
        """
        if self.is_new:
            return ""
        url = self.openbis.url
        location_part = self.physicalData.location.split("/")[-1]
        token = self.openbis.token

        file_links = {}
        for filepath in self.file_list:
            quoted_filepath = urllib.parse.quote(filepath, safe="")
            file_links[filepath] = (
                "/".join([url, "datastore_server", location_part, quoted_filepath])
                + "?sessionID="
                + token
            )

        return file_links

    @property
    def rel_file_links(self) -> Union[str, dict]:
        """Relative download URLs for all files in this dataset.

        Relative links can be embedded in HTML ``<img src="…">`` or
        ``<a href="…">`` elements inside an openBIS XML property so that
        pictures are displayed inline in the ELN-LIMS UI.

        Returns:
            A ``{relative_path: relative_url}`` dict, or ``""`` for new
            datasets.

        Example:
            >>> ds.rel_file_links
            {'original/DEFAULT/image.png': '/datastore_server/.../image.png'}
        """
        if self.is_new:
            return ""
        url = self.openbis.url
        location_part = self.physicalData.location.split("/")[-1]

        rel_file_links = {}
        for filepath in self.file_list:
            quoted_filepath = urllib.parse.quote(filepath, safe="")
            rel_file_links[filepath] = "/".join(
                ["/datastore_server", location_part, quoted_filepath]
            )

        return rel_file_links

    def get_files(self, start_folder: str = "/") -> DataFrame:
        """Return a :class:`~pandas.DataFrame` of all files in this dataset.

        Renames columns to ``isDirectory``, ``pathInDataSet``,
        ``fileSize``, and ``crc32Checksum`` for clarity.

        Args:
            start_folder: Filter to files whose path starts with this prefix.
                Defaults to ``"/"`` (all files).

        Returns:
            A :class:`~pandas.DataFrame` with the four standardised columns.

        Example:
            >>> ds.get_files()
            >>> ds.get_files(start_folder="/original/DEFAULT/")
        """
        if start_folder.startswith("/"):
            start_folder = start_folder[1:]
        file_list = self.get_dataset_files().df
        file_list[file_list["path"].str.startswith(start_folder)]
        new_file_list = file_list[
            ["directory", "path", "fileLength", "checksumCRC32"]
        ].rename(
            columns={
                "directory": "isDirectory",
                "path": "pathInDataSet",
                "fileLength": "fileSize",
                "checksumCRC32": "crc32Checksum",
            }
        )
        return new_file_list

    def _get_download_url(self) -> str:
        """Resolve the DSS download base URL for this dataset.

        Prefers the ``downloadUrl`` embedded in the dataset's ``dataStore``
        dict, falling back to the first configured datastore if absent.

        Returns:
            Download base URL string.
        """
        download_url = ""
        if "downloadUrl" in self.data["dataStore"]:
            download_url = self.data["dataStore"]["downloadUrl"]
        else:
            datastores = self.openbis.get_datastores()
            download_url = datastores["downloadUrl"][0]
        return download_url

    def get_file_list(self, recursive: bool = True, start_folder: str = "/") -> Any:
        """List files in this dataset using the legacy V1 DSS API.

        .. deprecated::
            Use :meth:`get_files` instead.

        Args:
            recursive: If ``True`` (default), list files in all
                subdirectories.
            start_folder: Root folder to start listing from.

        Returns:
            Raw list of file info dicts from the V1 API.

        Raises:
            ValueError: On API or network errors.
        """
        print("This method is deprecated. Consider using get_files() instead")
        request = {
            "method": "listFilesForDataSet",
            "params": [
                self.openbis.token,
                self.permId,
                start_folder,
                recursive,
            ],
            "id": "1",
        }
        download_url = self._get_download_url()
        resp = requests.post(
            download_url + "/datastore_server/rmi-dss-api-v1.json",
            json.dumps(request),
            verify=self.openbis.verify_certificates,
        )

        if resp.ok:
            data = resp.json()
            if "error" in data:
                raise ValueError("Error from openBIS: " + data["error"]["message"])
            elif "result" in data:
                return data["result"]
            else:
                raise ValueError(
                    "request to openBIS did not return either result nor error"
                )
        else:
            raise ValueError("internal error while performing post request")

    def _generate_plugin_request(
        self, dss: str, permId: Optional[PermId] = None
    ) -> dict:
        """Build a V1 ingestion plugin request for registering uploaded files.

        Args:
            dss: Data store server code.
            permId: Optional permId to assign to the new dataset.

        Returns:
            A V1 ``createReportFromAggregationService`` request dict.
        """
        sample_identifier = None
        if self.sample is not None:
            sample_identifier = self.sample.identifier

        experiment_identifier = None
        if self.experiment is not None:
            experiment_identifier = self.experiment.identifier

        parentIds = self.parents
        if parentIds is None:
            parentIds = []

        dataset_type = self.type.code
        properties = self.formatter.format(self.props.all_nonempty())

        request = {
            "method": "createReportFromAggregationService",
            "params": [
                self.openbis.token,
                dss,
                PYBIS_PLUGIN,
                {
                    "permId": permId,
                    "method": "insertDataSet",
                    "sampleIdentifier": sample_identifier,
                    "experimentIdentifier": experiment_identifier,
                    "dataSetType": dataset_type,
                    "folderName": self.folder,
                    "fileNames": self.files_in_wsp,
                    "isZipDirectoryUpload": self.isZipDirectoryUpload,
                    "properties": properties,
                    "parentIdentifiers": parentIds,
                },
            ],
        }
        return request

    def save(self, permId: Optional[PermId] = None) -> "DataSet":
        """Persist this dataset to openBIS (create or update).

        For **new physical** datasets, files are uploaded to the session
        workspace first, then registered.  For **container/link** datasets,
        the creation is done directly via the V3 API.  For **updates**, only
        the metadata (properties, relationships) are sent.

        Args:
            permId: Optional permId to request for the new dataset (V1 API
                only — ignored for V3 uploads).

        Returns:
            This :class:`DataSet` instance, updated with server-assigned
            fields.

        Raises:
            ValueError: If a mandatory property is missing, if no sample or
                experiment is linked, or if a physical dataset has no files.

        Example:
            >>> ds = openbis.new_dataset(
            ...     type="RAW_DATA", experiment="/S/P/E", files=["data.csv"]
            ... )
            >>> ds.save()
        """
        for prop_name, prop in self.props._property_names.items():
            if prop["mandatory"]:
                if (
                    getattr(self.props, prop_name) is None
                    or getattr(self.props, prop_name) == ""
                ):
                    raise ValueError(
                        f"Property '{prop_name}' is mandatory and must not be None"
                    )

        if self.is_new:
            data_stores = self.openbis.get_datastores()

            if self.sample is None and self.experiment is None:
                raise ValueError(
                    "A DataSet must be either connected to a Sample or an Experiment"
                )

            if self.kind == "PHYSICAL":
                if self.files is None or len(self.files) == 0:
                    raise ValueError(
                        "Cannot register a dataset without a file. Please provide at least one file"
                    )
                if self.openbis.get_server_information().is_version_greater_than(3, 5):
                    return self._upload_v3(data_stores)

                return self._upload_v1(permId, data_stores)
            else:
                if self.files is not None and len(self.files) > 0:
                    raise ValueError(
                        "DataSets of kind CONTAINER or LINK cannot contain data"
                    )

                request = self._new_attrs()

                if self.code is None or self.code == "":
                    request["params"][1][0]["autoGeneratedCode"] = True
                else:
                    request["params"][1][0]["autoGeneratedCode"] = False

                props = self.formatter.format(self.p._all_props())
                DSpermId = data_stores["code"][0]
                request["params"][1][0]["properties"] = props
                request["params"][1][0]["dataStoreId"] = {
                    "permId": DSpermId,
                    "@type": "as.dto.datastore.id.DataStorePermId",
                }

                version = self.openbis.get_server_information().openbis_version
                if version is not None:
                    if (
                        "SNAPSHOT" not in version
                        and not version.startswith("6")
                        and "UNKNOWN" not in version
                    ):
                        if (
                            request["method"]
                            in ("createDataSetTypes", "createDataSets")
                            and "metaData" in request["params"][1][0]
                        ):
                            del request["params"][1][0]["metaData"]

                resp = self.openbis._post_request(self.openbis.as_v3, request)

                if VERBOSE:
                    print("DataSet successfully created.")
                new_dataset_data = self.openbis.get_dataset(
                    resp[0]["permId"], only_data=True
                )
                self._set_data(new_dataset_data)
                return self

        else:
            request = self._up_attrs()
            props = self.formatter.format(self.p._all_props())
            request["params"][1][0]["properties"] = props

            version = self.openbis.get_server_information().openbis_version
            if version is not None:
                if (
                    "SNAPSHOT" not in version
                    and not version.startswith("6")
                    and "UNKNOWN" not in version
                ):
                    if (
                        request["method"] in ("updateDataSetTypes", "updateDataSets")
                        and "metaData" in request["params"][1][0]
                    ):
                        del request["params"][1][0]["metaData"]

            self.openbis._post_request(self.openbis.as_v3, request)
            if VERBOSE:
                print("DataSet successfully updated.")

    def _upload_v1(self, permId: Optional[PermId], datastores: Any) -> "DataSet":
        """Upload files via the V1 session-workspace API and register the dataset.

        Args:
            permId: Optional permId to assign to the new dataset.
            datastores: Datastore info dict from ``openbis.get_datastores()``.

        Returns:
            This :class:`DataSet` instance, updated with server-assigned data.

        Raises:
            ValueError: If the ingestion plugin returns an error.
        """
        self.upload_files_v1(
            datastore_url=datastores["downloadUrl"][0],
            files=self.files,
            folder="",
            wait_until_finished=True,
        )

        request = self._generate_plugin_request(
            dss=datastores["code"][0],
            permId=permId,
        )
        resp = self.openbis._post_request(self.openbis.reg_v1, request)
        if resp["rows"][0][0]["value"] == "OK":
            permId = resp["rows"][0][2]["value"]
            if permId is None or permId == "":
                self.__dict__["is_new"] = False
                if VERBOSE:
                    print(
                        "DataSet successfully created. Because you connected to an openBIS version older than 16.05.04, you cannot update the object."
                    )
            else:
                new_dataset_data = self.openbis.get_dataset(permId, only_data=True)
                self._set_data(new_dataset_data)
                if VERBOSE:
                    print("DataSet successfully created.")
                return self
        else:
            print(json.dumps(request))
            raise ValueError(
                "Error while creating the DataSet: " + resp["rows"][0][1]["value"]
            )

    def _upload_v3(self, data_stores: Any) -> "DataSet":
        """Upload files using the V3 DSS upload API and register the dataset.

        Args:
            data_stores: Datastore info dict from ``openbis.get_datastores()``.

        Returns:
            This :class:`DataSet` instance, updated with server-assigned data.

        Raises:
            ValueError: If the server returns an error.
        """
        datastore_url = data_stores["downloadUrl"][0]
        upload_id = self.upload_files_v3(
            datastore_url=datastore_url,
            files=self.files,
            wait_until_finished=True,
        )
        type_code = self.type.code
        if type_code.startswith("$"):
            type_code = type_code[1:]

        props = self.formatter.format(self.props.all_nonempty())
        param = {
            "@type": "dss.dto.dataset.create.UploadedDataSetCreation",
            "@id": "1",
            "typeId": {
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "@id": "2",
                "permId": type_code,
                "entityKind": "DATA_SET",
            },
            "properties": props,
            "parentIds": [],
            "uploadId": upload_id,
        }

        if self.experiment is not None:
            param["experimentId"] = {
                "@type": "as.dto.experiment.id.ExperimentIdentifier",
                "@id": "3",
                "identifier": self.experiment.identifier,
            }
        if self.sample is not None:
            param["sampleId"] = {
                "@type": "as.dto.sample.id.SamplePermId",
                "@id": "4",
                "permId": self.sample.permId,
            }
        parent_ids = self.parents
        if parent_ids is None:
            parent_ids = []
        counter = 5
        for parent_id in parent_ids:
            param["parentIds"] += [
                {
                    "@type": "as.dto.dataset.id.DataSetPermId",
                    "@id": str(counter),
                    "permId": parent_id,
                }
            ]
            counter += 1

        request = {
            "method": "createUploadedDataSet",
            "params": [self.openbis.token, param],
        }

        resp = self.openbis._post_request_full_url(
            urljoin(datastore_url, self.openbis.dss_v3), request
        )
        if "permId" in resp:
            permId = resp["permId"]
            if permId is None or permId == "":
                self.__dict__["is_new"] = False
                if VERBOSE:
                    print(
                        "DataSet successfully created. Because you connected to an openBIS version older than 16.05.04, you cannot update the object."
                    )
            else:
                new_dataset_data = self.openbis.get_dataset(permId, only_data=True)
                self._set_data(new_dataset_data)
                if VERBOSE:
                    print("DataSet successfully created.")
                return self
        else:
            print(json.dumps(request))
            raise ValueError(
                "Error while creating the DataSet: " + resp["rows"][0][1]["value"]
            )

    def zipit(self, file_or_folder: str, zipf: Any) -> None:
        """Add a file or directory to a zipfile instance.

        Files are stored in the zip root.  Directories are stored with
        their top-level folder name as the root inside the zip.

        Args:
            file_or_folder: Path to a file or directory to add.
            zipf: An open :class:`zipfile.ZipFile` instance to write into.
        """
        if os.path.isfile(file_or_folder):
            (realpath, filename) = os.path.split(os.path.realpath(file_or_folder))
            zipf.write(file_or_folder, filename)
        elif os.path.isdir(file_or_folder):
            (head, tail) = os.path.split(os.path.realpath(file_or_folder))
            for dirpath, dirnames, filenames in os.walk(file_or_folder):
                realpath = os.path.realpath(dirpath)
                for filename in filenames:
                    zipf.write(
                        os.path.relpath(
                            os.path.join(dirpath, filename),
                            os.path.join(filename, ".."),
                        ),
                        os.path.join(realpath[len(head) + 1 :], filename),
                    )

    def upload_files_v1(
        self,
        datastore_url: Optional[str] = None,
        files: Optional[Any] = None,
        folder: Optional[str] = None,
        wait_until_finished: bool = False,
    ) -> list:
        """Upload files to the DSS session workspace using the V1 API.

        Directories in ``files`` are automatically zipped before upload.
        Files are uploaded in parallel using a :class:`DataSetUploadQueue`.

        Args:
            datastore_url: DSS base URL.  Defaults to the configured DSS URL.
            files: File path(s) to upload.  Required.
            folder: Session-workspace folder name.  Defaults to a timestamp.
            wait_until_finished: Block until all uploads complete.

        Returns:
            List of file paths as stored in the session workspace.

        Raises:
            ValueError: If no files are provided.
        """
        if datastore_url is None:
            datastore_url = self.openbis._get_dss_url()
        if files is None:
            raise ValueError("Please provide a filename.")

        if folder is None:
            folder = time.strftime("%Y-%m-%d_%H-%M-%S")

        if isinstance(files, str):
            files = [files]

        contains_dir = False
        for f in files:
            if os.path.isdir(f):
                contains_dir = True

        if contains_dir:
            file_ending = "".join(
                random.choice(
                    "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                )
                for i in range(6)
            )
            filename = time.strftime("%Y-%m-%d_%H-%M-%S") + file_ending + ".zip"
            buf = ZipBuffer(
                openbis_obj=self.openbis, host=datastore_url, filename=filename
            )
            zipf = zipfile.ZipFile(file=buf, mode="w", compression=zipfile.ZIP_DEFLATED)
            for file_or_folder in files:
                self.zipit(file_or_folder, zipf)
            self.__dict__["files_in_wsp"] = [filename]
            self.__dict__["isZipDirectoryUpload"] = True
            return self.files_in_wsp

        with DataSetUploadQueue() as queue:
            real_files = []
            for filename in files:
                if os.path.isdir(filename):
                    real_files.extend(
                        [
                            os.path.join(dp, f)
                            for dp, dn, fn in os.walk(os.path.expanduser(filename))
                            for f in fn
                        ]
                    )
                else:
                    real_files.append(os.path.join(filename))

            for filename in real_files:
                file_in_wsp = os.path.join(folder, os.path.basename(filename))
                url_filename = os.path.join(
                    folder, urllib.parse.quote(os.path.basename(filename))
                )
                self.files_in_wsp.append(file_in_wsp)

                upload_url = (
                    datastore_url
                    + "/datastore_server/session_workspace_file_upload"
                    + "?filename="
                    + url_filename
                    + "&id=1"
                    + "&startByte=0&endByte=0"
                    + "&sessionID="
                    + self.openbis.token
                )
                queue.put([upload_url, filename, self.openbis.verify_certificates])

            if wait_until_finished:
                queue.join()

            return self.files_in_wsp

    def upload_files_v3(
        self,
        files: Any,
        datastore_url: Optional[str] = None,
        folder: Optional[str] = None,
        wait_until_finished: bool = False,
    ) -> str:
        """Upload files to the DSS session workspace using the V3 multipart API.

        Large files (> 10 MB) are split into 10 MB chunks and uploaded in
        parallel.  Each file is verified for modification before and after
        upload.

        Args:
            files: File path(s) to upload.  Required.
            datastore_url: DSS base URL.  Defaults to the configured DSS URL.
            folder: Sub-folder within the upload ID directory.
            wait_until_finished: Block until all uploads complete.

        Returns:
            The upload ID (UUID) string used to register the dataset.

        Raises:
            ValueError: If no files are provided, or if a file changes during
                upload (detected by :func:`_assert_unchanged`).
        """
        if datastore_url is None:
            datastore_url = self.openbis._get_dss_url()
        if files is None:
            raise ValueError("Please provide a filename.")

        if isinstance(files, str):
            files = [files]

        upload_id = str(uuid.uuid4())
        if len(files) == 1:
            if folder is None:
                folder = upload_id
            else:
                folder = os.path.join(upload_id, folder)

        else:
            if folder is None:
                folder = os.path.join(upload_id, "default")
            else:
                folder = os.path.join(upload_id, folder)

        if len(files) == 0:
            raise ValueError("Please provide a filename.")

        with DataSetUploadQueueNew(datastore_url) as queue:
            real_files = []
            for filename in files:
                if os.path.isdir(filename):
                    pardir = os.path.join(filename, os.pardir)
                    for root, dirs, files in os.walk(os.path.expanduser(filename)):
                        path = os.path.relpath(root, pardir)
                        for file in files:
                            real_files.append((path, os.path.join(root, file)))
                        if not files:
                            real_files.append((path, ""))
                else:
                    real_files.append(("", os.path.join(filename)))

            for filename in real_files:
                file_in_wsp = os.path.join(
                    folder, filename[0], os.path.basename(filename[1])
                )
                url_filename = os.path.join(
                    folder,
                    filename[0],
                    urllib.parse.quote(os.path.basename(filename[1])),
                )
                url_filename = "/".join(url_filename.split("\\"))
                self.files_in_wsp.append(file_in_wsp)

                is_empty_folder = filename[1] == ""
                if is_empty_folder:
                    upload_url = (
                        f"{datastore_url}{SESSION_WORKSPACE}"
                        f"?filename={url_filename}"
                        f"&id={1}"
                        f"&startByte={0}&endByte={0}"
                        f"&emptyFolder={True}"
                        f"&sessionID={self.openbis.token}"
                    )
                    queue.put(
                        [
                            upload_url,
                            filename,
                            self.openbis.verify_certificates,
                            True,
                            False,
                            [],
                            None,
                        ]
                    )
                else:
                    expected_stat = _stat_snapshot(filename[1])

                    file_size = os.path.getsize(filename[1])
                    count = 1
                    size = 1024 * 1024 * 10  # 10MB
                    if file_size > size:
                        for i in range(0, file_size, size):
                            start_byte = i
                            end_byte = min(i + size - 1, file_size)
                            upload_url = (
                                f"{datastore_url}{SESSION_WORKSPACE}"
                                f"?filename={url_filename}"
                                f"&id={count}"
                                f"&startByte={start_byte}&endByte={end_byte}"
                                f"&emptyFolder={False}"
                                f"&sessionID={self.openbis.token}"
                            )
                            queue.put(
                                [
                                    upload_url,
                                    filename,
                                    self.openbis.verify_certificates,
                                    False,
                                    True,
                                    [start_byte, end_byte],
                                    expected_stat,
                                ]
                            )
                            count += 1
                    else:
                        upload_url = (
                            datastore_url
                            + "/datastore_server/session_workspace_file_upload"
                            + "?filename="
                            + url_filename
                            + "&id="
                            + str(count)
                            + "&startByte=0&endByte="
                            + str(file_size)
                            + "&emptyFolder=False"
                            + "&sessionID="
                            + self.openbis.token
                        )
                        queue.put(
                            [
                                upload_url,
                                filename,
                                self.openbis.verify_certificates,
                                False,
                                False,
                                [],
                                expected_stat,
                            ]
                        )

            if wait_until_finished:
                try:
                    queue.join()
                except BaseException as e:
                    raise e

            return upload_id


class PropagatingThread(Thread):
    """A :class:`~threading.Thread` subclass that re-raises exceptions in the caller.

    When a thread raises an exception, it is stored and re-raised when
    :meth:`join` is called.  This allows callers to detect and handle
    errors from worker threads.
    """

    exc: Optional[BaseException]
    ret: Any

    def run(self) -> None:
        """Run the thread target, capturing any exception."""
        self.exc = None
        try:
            self.ret = self._target(*self._args, **self._kwargs)
        except BaseException as e:
            self.exc = e

    def join(self, timeout: Optional[float] = None) -> Any:
        """Join the thread and re-raise any captured exception.

        Args:
            timeout: Optional timeout in seconds.

        Returns:
            The return value of the thread target.

        Raises:
            BaseException: Any exception raised by the thread target.
        """
        super(PropagatingThread, self).join(timeout)
        if self.exc:
            raise self.exc
        return self.ret


def _stat_snapshot(path: str) -> tuple:
    """Take a (size, mtime_ns) snapshot of a file for change detection.

    Args:
        path: File path to stat.

    Returns:
        A ``(file_size, mtime_ns)`` tuple.
    """
    st = os.stat(path)
    return (st.st_size, getattr(st, "st_mtime_ns", int(st.st_mtime * 1e9)))


def _assert_unchanged(path: str, expected: tuple) -> None:
    """Raise an error if a file has changed since the snapshot was taken.

    Compares current ``(size, mtime_ns)`` against ``expected``.

    Args:
        path: File path to check.
        expected: ``(size, mtime_ns)`` tuple from :func:`_stat_snapshot`.

    Raises:
        ValueError: If the file size or modification time has changed.
    """
    size0, mtime0 = expected
    st = os.stat(path)
    size = st.st_size
    mtime = getattr(st, "st_mtime_ns", int(st.st_mtime * 1e9))
    if size != size0 or mtime != mtime0:
        raise ValueError(f"File changed during upload: {path}")


class DataSetUploadQueueNew:
    """Multi-threaded V3 file upload queue for openBIS datasets.

    Supports both whole-file and chunked (partial) uploads, empty-folder
    creation, and pre/post-upload file-change detection.  Uses HTTP retry
    logic for resilience against transient DSS errors.

    Upload errors cancel remaining work and are re-raised from :meth:`join`.

    Example:
        >>> with DataSetUploadQueueNew(dss_url) as queue:
        ...     queue.put([url, filename, verify_certs, False, False, [], stat])
        ...     queue.join()
    """

    upload_queue: Queue
    workers: int
    session: requests.Session
    threads: list[PropagatingThread]
    exceptions: Queue
    cancelled: threading.Event
    _drain_lock: threading.Lock

    def create_session(self, url_base: str) -> requests.Session:
        """Create a requests session with automatic retry logic.

        Args:
            url_base: Base URL used to mount the retry adapter.

        Returns:
            A configured :class:`requests.Session`.
        """
        session = requests.Session()
        retries = Retry(
            total=REQUEST_RETRIES_COUNT,
            backoff_factor=1,
            status_forcelist=[502, 503, 504],
        )
        session.mount(url_base, HTTPAdapter(max_retries=retries))
        return session

    def __init__(self, url_base: str, workers: int = 10) -> None:
        """Initialise the upload queue and start worker threads.

        Args:
            url_base: DSS base URL (used for the retry session).
            workers: Number of parallel upload threads (default 10).
        """
        self.upload_queue = Queue()
        self.workers = workers
        self.session = self.create_session(url_base)
        self.threads = []
        self.exceptions = Queue()
        self.cancelled = threading.Event()
        self._drain_lock = threading.Lock()
        for t in range(workers):
            t = PropagatingThread(target=self.upload_file)
            self.threads += [t]
            t.start()

    def __enter__(self, *args: Any, **kwargs: Any) -> "DataSetUploadQueueNew":
        return self

    def __exit__(self, *args: Any, **kwargs: Any) -> None:
        """Shut down worker threads cleanly."""
        for i in range(self.workers):
            self.upload_queue.put(None)
        for t in self.threads:
            t.join()

    def put(self, things: list) -> None:
        """Enqueue a file upload task.

        Args:
            things: A list with elements
                ``[upload_url, filename, verify_certs, is_empty_folder,
                partial, bytes_range, expected_stat]``.
        """
        self.upload_queue.put(things)

    def join(self) -> None:
        """Block until all queued uploads have completed.

        Raises:
            Exception: The first exception raised by any worker thread.
        """
        self.upload_queue.join()
        if not self.exceptions.empty():
            raise self.exceptions.get()
        for t in self.threads:
            if getattr(t, "exc", None):
                raise t.exc

    def upload_file(self) -> Any:
        """Worker loop: dequeue and execute upload tasks.

        Handles whole-file, chunked, and empty-folder uploads.  On failure,
        sets the cancellation flag, drains remaining tasks, and stores the
        exception for :meth:`join` to re-raise.
        """
        while True:
            item = self.upload_queue.get()
            if item is None:
                self.upload_queue.task_done()
                break

            if self.cancelled.is_set():
                self.upload_queue.task_done()
                continue

            (
                upload_url,
                filename,
                verify_certificates,
                is_empty_folder,
                partial,
                bytes_range,
                expected_stat,
            ) = item

            try:
                if is_empty_folder:
                    resp = self.session.post(upload_url, verify=verify_certificates)
                    resp.raise_for_status()
                else:
                    path = filename[1]

                    if expected_stat is not None:
                        _assert_unchanged(path, expected_stat)

                    if partial:
                        with open(path, "rb") as f:
                            f.seek(bytes_range[0])
                            data = f.read(bytes_range[1] - bytes_range[0] + 1)
                            resp = self.session.post(
                                upload_url, data=data, verify=verify_certificates
                            )
                            resp.raise_for_status()

                        if expected_stat is not None:
                            _assert_unchanged(path, expected_stat)
                    else:
                        file_size = os.path.getsize(path)
                        with open(path, "rb") as f:
                            resp = self.session.post(
                                upload_url, data=f, verify=verify_certificates
                            )
                            resp.raise_for_status()
                            data = resp.json()
                            if file_size != int(data["size"]):
                                raise ValueError(
                                    f"size of file uploaded: {file_size} != data received: {int(data['size'])}"
                                )
                        if expected_stat is not None:
                            _assert_unchanged(path, expected_stat)

            except BaseException as e:
                first = False
                with self._drain_lock:
                    if not self.cancelled.is_set():
                        self.cancelled.set()
                        first = True
                        if self.exceptions.empty():
                            self.exceptions.put(e)

                if first:
                    while True:
                        try:
                            leftover = self.upload_queue.get_nowait()
                        except Empty:
                            break
                        else:
                            self.upload_queue.task_done()

                self.upload_queue.task_done()
                return

            else:
                self.upload_queue.task_done()
        return True


class DataSetUploadQueue:
    """Multi-threaded V1 file upload queue for openBIS datasets.

    Simpler than :class:`DataSetUploadQueueNew` — no chunking, no retry
    logic, no cancellation.  Used for openBIS versions < 3.5.

    Example:
        >>> with DataSetUploadQueue() as queue:
        ...     queue.put([url, filename, verify_certs])
        ...     queue.join()
    """

    upload_queue: Queue
    workers: int
    multipart: bool

    def __init__(self, workers: int = 20, multipart: bool = False) -> None:
        """Initialise the upload queue and start worker threads.

        Args:
            workers: Number of parallel upload threads (default 20).
            multipart: If ``True``, use multipart form-data upload instead
                of raw body upload.
        """
        self.upload_queue = Queue()
        self.workers = workers
        self.multipart = multipart

        for t in range(workers):
            t = Thread(target=self.upload_file)
            t.start()

    def __enter__(self, *args: Any, **kwargs: Any) -> "DataSetUploadQueue":
        return self

    def __exit__(self, *args: Any, **kwargs: Any) -> None:
        """Send stop sentinels to all worker threads."""
        for i in range(self.workers):
            self.upload_queue.put(None)

    def put(self, things: list) -> None:
        """Enqueue a file upload task.

        Args:
            things: A list with elements
                ``[upload_url, filename, verify_certificates]``.
        """
        self.upload_queue.put(things)

    def join(self) -> None:
        """Block until all queued uploads have completed."""
        self.upload_queue.join()

    def upload_file(self) -> None:
        """Worker loop: dequeue and upload files one at a time.

        Verifies that the uploaded file size matches the server-reported
        size after each upload.
        """
        while True:
            queue_item = self.upload_queue.get()
            if queue_item is None:
                break
            upload_url, filename, verify_certificates = queue_item

            file_size = os.path.getsize(filename)

            if self.multipart is True:
                file = {filename: open(filename, "rb")}
                resp = requests.post(upload_url, files=file, verify=verify_certificates)
                resp.raise_for_status()
            else:
                with open(filename, "rb") as f:
                    resp = requests.post(upload_url, data=f, verify=verify_certificates)
                    resp.raise_for_status()
                    data = resp.json()
                    if file_size != int(data["size"]):
                        raise ValueError(
                            f"size of file uploaded: {file_size} != data received: {int(data['size'])}"
                        )

            self.upload_queue.task_done()


class ZipBuffer(object):
    """A file-like write buffer that streams zip data directly to the DSS.

    :class:`zipfile.ZipFile` calls :meth:`write` to store compressed bytes.
    Instead of buffering to disk, each :meth:`write` call is forwarded as an
    HTTP POST to the DSS session workspace.

    Used by :meth:`DataSet.upload_files_v1` when the input contains
    directories (V1 API only).

    Args:
        openbis_obj: The :class:`~pybis.Openbis` connection instance.
        host: DSS base URL.
        filename: Target filename in the session workspace.
    """

    openbis: Any
    startByte: int
    endByte: int
    filename: str
    upload_url: str
    session: Session

    def __init__(self, openbis_obj: Any, host: str, filename: str) -> None:
        self.openbis = openbis_obj
        self.startByte = 0
        self.endByte = 0
        self.filename = filename
        self.upload_url = (
            host + "/datastore_server/session_workspace_file_upload?"
            "filename={}"
            "&id=1"
            "&startByte={}"
            "&endByte={}"
            "&sessionID={}"
        )
        self.session = Session()

    def write(self, data: bytes) -> None:
        """Write a chunk of zip data to the DSS session workspace.

        Retries up to 10 times on non-200 responses.

        Args:
            data: Bytes to upload.

        Raises:
            Exception: If the upload fails after 10 attempts.
        """
        self.startByte = self.endByte
        self.endByte += len(data)
        attempts = 0

        while True:
            attempts += 1
            resp = self.session.post(
                url=self.upload_url.format(
                    self.filename, self.startByte, self.endByte, self.openbis.token
                ),
                data=data,
                verify=self.openbis.verify_certificates,
            )
            if resp.status_code == 200:
                break
            if attempts > 10:
                raise Exception("Upload failed after more than 10 attempts")

    def tell(self) -> int:
        """Return the current byte position in the stream.

        Returns:
            Total bytes written so far.
        """
        return self.endByte

    def flush(self) -> None:
        """Close the underlying session (no buffered data to flush)."""
        self.session.close()
        pass


class DataSetDownloadQueue:
    """Multi-threaded V1 file download queue for openBIS datasets.

    Downloads files in parallel using a thread pool.  If a file's actual
    size does not match the expected size, a warning is printed (or the
    file is collected in :attr:`files_with_wrong_length` when
    ``collect_files_with_wrong_length=True``).

    Example:
        >>> with DataSetDownloadQueue(workers=5) as queue:
        ...     queue.put([url, filename, dest, size, verify, "wb"])
        ...     queue.join()
    """

    collect_files_with_wrong_length: bool
    workers: int
    download_queue: Queue
    files_with_wrong_length: list[str]

    def __init__(
        self,
        workers: int = 20,
        collect_files_with_wrong_length: bool = False,
    ) -> None:
        """Initialise the download queue and start worker threads.

        Args:
            workers: Number of parallel download threads (default 20).
            collect_files_with_wrong_length: If ``True``, files whose
                downloaded size does not match the expected size are
                collected in :attr:`files_with_wrong_length` instead of
                printing a warning.
        """
        self.collect_files_with_wrong_length = collect_files_with_wrong_length
        self.workers = workers
        self.download_queue = Queue()
        self.files_with_wrong_length = []

        for i in range(workers):
            thread = Thread(target=self.download_file)
            thread.start()

    def __enter__(self, *args: Any, **kwargs: Any) -> "DataSetDownloadQueue":
        return self

    def __exit__(self, *args: Any, **kwargs: Any) -> None:
        """Send stop sentinels to all worker threads."""
        for i in range(self.workers):
            self.download_queue.put(None)

    def put(self, things: list) -> None:
        """Enqueue a file download task.

        Args:
            things: A list with elements
                ``[url, filename, filename_dest, file_size,
                verify_certificates, write_mode]``.
        """
        self.download_queue.put(things)

    def join(self) -> None:
        """Block until all queued downloads have completed."""
        self.download_queue.join()

    def download_file(self) -> None:
        """Worker loop: dequeue and download files one at a time.

        Creates the destination directory tree as needed, streams the
        download in 1 MB chunks, and verifies the final file size.
        """
        while True:
            try:
                queue_item = self.download_queue.get()
                if queue_item is None:
                    break
                (
                    url,
                    filename,
                    filename_dest,
                    file_size,
                    verify_certificates,
                    write_mode,
                ) = queue_item
                os.makedirs(os.path.dirname(filename_dest), exist_ok=True)

                r = requests.get(url, stream=True, verify=verify_certificates)
                if r.ok == False:
                    raise ValueError(
                        f"Could not download from {url}: HTTP {r.status_code}. Reason: {r.reason}"
                    )

                with open(filename_dest, write_mode) as fh:
                    for chunk in r.iter_content(chunk_size=1024 * 1024):
                        if chunk:
                            fh.write(chunk)

                r.raise_for_status()
                actual_file_size = os.path.getsize(filename_dest)
                if actual_file_size != int(file_size):
                    if self.collect_files_with_wrong_length:
                        self.files_with_wrong_length.append(filename)
                    else:
                        print(
                            f"WARNING! File {filename_dest} has the wrong length: Expected: {int(file_size)} Actual size: {actual_file_size}"
                        )
                        print(
                            "REASON: The connection has been silently dropped upstreams.",
                            "Please check the http timeout settings of the openBIS datastore server",
                        )
            except Exception as err:
                print(f"ERROR while writing file {filename_dest}: {err}")

            finally:
                self.download_queue.task_done()


class PhysicalData:
    """Physical storage metadata for a :class:`DataSet`.

    Returned by :attr:`DataSet.physicalData`.  Provides access to storage
    location, archiving status, and other low-level data store attributes.

    Attributes:
        speedHint (Optional[int]): Storage speed hint.
        complete (bool): Whether all files are present.
        shareId (str): Data store share identifier.
        size (int): Total dataset size in bytes.
        fileFormatType (str): File format type code.
        storageFormat (str): Storage format (e.g. ``"PROPRIETARY"``).
        location (str): Relative path within the data store share.
        presentInArchive (bool): Whether a copy exists in the archive.
        storageConfirmation (bool): Whether storage has been confirmed.
        locatorType (str): Locator type (e.g. ``"RELATIVE_IN_SHARE"``).
        status (str): Current status (``"AVAILABLE"``, ``"ARCHIVED"``, …).
    """

    data: dict[str, Any]
    attrs: list[str]

    def __init__(self, data: Optional[dict] = None) -> None:
        """Initialise PhysicalData from a raw API dict.

        Args:
            data: Raw ``physicalData`` dict from the V3 API response.
                If ``None``, defaults to an empty dict.
        """
        if data is None:
            data = {}
        self.data = data
        self.attrs = [
            "speedHint",
            "complete",
            "shareId",
            "size",
            "fileFormatType",
            "storageFormat",
            "location",
            "presentInArchive",
            "storageConfirmation",
            "locatorType",
            "status",
        ]

    def __dir__(self) -> list[str]:
        return self.attrs

    def __getattr__(self, name: str) -> Any:
        if name in self.attrs:
            if name in self.data:
                return self.data[name]

    def __getitem__(self, key: str) -> Any:
        if key in self.attrs:
            if key in self.data:
                return self.data[key]

    def _repr_html_(self) -> str:
        html = """
            <table border="1" class="dataframe">
            <thead>
                <tr style="text-align: right;">
                <th>attribute</th>
                <th>value</th>
                </tr>
            </thead>
            <tbody>
        """

        for attr in self.attrs:
            html += f"<tr> <td>{attr}</td> <td>{getattr(self, attr, '')}</td> </tr>"

        html += """
            </tbody>
            </table>
        """
        return html

    def __repr__(self) -> str:
        headers = ["attribute", "value"]
        lines = []
        for attr in self.attrs:
            lines.append([attr, getattr(self, attr, "")])
        return tabulate(lines, headers=headers)


class LinkedData:
    """Linked-data metadata for a :class:`DataSet` of kind ``"LINK"``.

    Returned by :attr:`DataSet.linkedData`.  Provides access to the external
    code and content copies that reference data in an external data
    management system.

    Attributes:
        externalCode (str): External identifier of the dataset.
        contentCopies (list): List of content copy dicts, each describing a
            specific copy in an external DMS with a path and credentials.
    """

    data: Any
    attrs: list[str]

    def __init__(self, data: Optional[Any] = None) -> None:
        """Initialise LinkedData.

        Args:
            data: Raw ``linkedData`` dict from the V3 API response.
                If ``None``, defaults to an empty list.
        """
        self.data = data if data is not None else []
        self.attrs = ["externalCode", "contentCopies"]

    def __dir__(self) -> list[str]:
        return self.attrs

    def __getattr__(self, name: str) -> Any:
        if name in self.attrs:
            if name in self.data:
                return self.data[name]
        else:
            return ""
