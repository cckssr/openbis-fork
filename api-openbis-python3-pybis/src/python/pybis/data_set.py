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
"""Creation, update, and file search of git link data sets.

Created by Chandrasekhar Ramakrishnan on 2017-04-05.
Copyright (c) 2017 Chandrasekhar Ramakrishnan. All rights reserved.
"""

from typing import Any, Optional


def transfer_to_file_creation(
    content: "dict[str, Any]",
    file_creation: "dict[str, Any]",
    key: str,
    file_creation_key: Optional[str] = None,
) -> None:
    """Copy one field from a content dict into a file-creation DTO, if set."""
    if file_creation_key is None:
        file_creation_key = key
    if content.get(key) is not None:
        file_creation[file_creation_key] = content[key]


class GitDataSetCreation(object):
    """Builds and executes the creation of a git link data set."""

    def __init__(
        self,
        openbis: Any,
        data_set_type: str,
        path: str,
        commit_id: str,
        repository_id: str,
        dms: Any,
        sample: Optional[Any] = None,
        experiment: Optional[Any] = None,
        properties: "Optional[dict[str, Any]]" = None,
        dss_code: Optional[str] = None,
        parents: Optional[Any] = None,
        data_set_code: Optional[str] = None,
        contents: "Optional[list[dict[str, Any]]]" = None,
    ) -> None:
        """Initialize the command object with the necessary parameters.

        Args:
            openbis: The openBIS API object.
            data_set_type: The type of the data set.
            path: The path to the git repository.
            commit_id: The git commit id.
            repository_id: The git repository id - same for copies.
            dms: An external data managment system object or external_dms_id.
            sample: A sample object or sample id.
            experiment: An experiment or experiment id.
            properties: Properties for the data set.
            dss_code: Code for the DSS -- defaults to the first dss if none
                is supplied.
            parents: Parents for the data set.
            data_set_code: A data set code -- used if provided, otherwise
                generated on the server.
            contents: A list of dicts that describe the contents:
                ``{'fileLength': ..., 'crc32': ..., 'checksum': ...,
                'checksumType': ..., 'directory': ..., 'path': ...}``.
        """
        self.openbis = openbis
        self.data_set_type = data_set_type
        self.path = path
        self.commit_id = commit_id
        self.repository_id = repository_id
        self.dms = dms
        self.sample = sample
        self.experiment = experiment
        self.properties = properties if properties is not None else {}
        self.dss_code = dss_code
        self.parents = parents
        self.data_set_code = data_set_code
        self.contents = contents if contents is not None else []

    def new_git_data_set(self) -> Any:
        """Create a link data set.

        Returns:
            A DataSet object.
        """
        data_set_creation = self.data_set_metadata_creation()
        file_metadata = self.data_set_file_metadata()
        if not file_metadata:
            return self.create_pure_metadata_data_set(data_set_creation)
        return self.create_mixed_data_set(data_set_creation, file_metadata)

    def create_pure_metadata_data_set(self, data_set_creation: "dict[str, Any]") -> Any:
        """Register a metadata-only link data set and return it."""
        # register the files in openBIS
        request = {
            "method": "createDataSets",
            "params": [self.openbis.token, [data_set_creation]],
        }

        # noinspection PyProtectedMember
        resp = self.openbis._post_request(self.openbis.as_v3, request)
        return self.openbis.get_dataset(resp[0]["permId"])

    def create_mixed_data_set(
        self,
        metadata_creation: "dict[str, Any]",
        file_metadata: "list[dict[str, Any]]",
    ) -> Any:
        """Register a link data set with file metadata and return it."""
        data_set_creation = {
            "fileMetadata": file_metadata,
            "metadataCreation": metadata_creation,
            "@type": "dss.dto.dataset.create.FullDataSetCreation",
        }

        # register the files in openBIS
        request = {
            "method": "createDataSets",
            "params": [self.openbis.token, [data_set_creation]],
        }

        server_url = self.data_store_url(metadata_creation["dataStoreId"]["permId"])
        # noinspection PyProtectedMember
        resp = self.openbis._post_request_full_url(server_url, request)
        return self.openbis.get_dataset(resp[0]["permId"])

    def data_store_url(self, dss_code: str) -> str:
        """Return the V3 JSON-RPC endpoint of the data store ``dss_code``."""
        data_stores = self.openbis.get_datastores()
        data_store = data_stores[data_stores["code"] == dss_code]
        return f"{data_store['downloadUrl'][0]}/datastore_server/rmi-data-store-server-v3.json"

    def data_set_metadata_creation(self) -> "dict[str, Any]":
        """Create the respresentation of the data set metadata."""
        dss_code = self.dss_code
        if dss_code is None:
            dss_code = self.openbis.get_datastores()["code"][0]

        dms_id = self.openbis.external_data_managment_system_to_dms_id(self.dms)
        parents = self.parents
        parentIds = []
        if parents is not None:
            if not isinstance(parents, list):
                parents = [parents]
            parentIds = [
                self.openbis.data_set_to_data_set_id(parent) for parent in parents
            ]
        data_set_creation: dict[str, Any] = {
            "linkedData": {
                "@type": "as.dto.dataset.create.LinkedDataCreation",
                "contentCopies": [
                    {
                        "@type": "as.dto.dataset.create.ContentCopyCreation",
                        "path": self.path,
                        "gitCommitHash": self.commit_id,
                        "gitRepositoryId": self.repository_id,
                        "externalDmsId": dms_id,
                    }
                ],
            },
            "typeId": {
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "permId": self.data_set_type,
            },
            "dataStoreId": {
                "permId": dss_code,
                "@type": "as.dto.datastore.id.DataStorePermId",
            },
            "parentIds": parentIds,
            "measured": False,
            "properties": self.properties,
            "@type": "as.dto.dataset.create.DataSetCreation",
        }

        if self.sample is not None:
            sample_id = self.openbis.sample_to_sample_id(self.sample)
            data_set_creation["sampleId"] = sample_id
        elif self.experiment is not None:
            experiment_id = self.openbis.experiment_to_experiment_id(self.experiment)
            data_set_creation["experimentId"] = experiment_id

        if self.data_set_code is not None:
            data_set_creation["code"] = self.data_set_code
            data_set_creation["autoGeneratedCode"] = False
        else:
            data_set_creation["autoGeneratedCode"] = True

        return data_set_creation

    def data_set_file_metadata(self) -> "list[dict[str, Any]]":
        """Create a representation of the file metadata."""
        return [self.as_file_metadata(c) for c in self.contents]

    def as_file_metadata(self, content: "dict[str, Any]") -> "dict[str, Any]":
        """Map one content description onto the DSS file-creation fields."""
        # The DSS objects do not use type
        # result = {"@type": "dss.dto.datasetfile.DataSetFileCreation"}
        result: dict[str, Any] = {}
        transfer_to_file_creation(content, result, "fileLength")
        transfer_to_file_creation(content, result, "crc32", "checksumCRC32")
        transfer_to_file_creation(content, result, "checksum", "checksum")
        transfer_to_file_creation(content, result, "checksumType", "checksumType")
        transfer_to_file_creation(content, result, "directory")
        transfer_to_file_creation(content, result, "path")
        return result


class GitDataSetUpdate(object):
    """Adds or removes content copies of an existing link data set."""

    def __init__(self, openbis: Any, data_set_id: str) -> None:
        """Initialize the command object with the necessary parameters.

        Args:
            openbis: The openBIS API object.
            data_set_id: Id of the data set to be updated.
        """
        self.openbis = openbis
        self.data_set_id = data_set_id

    def new_content_copy(
        self, path: str, commit_id: str, repository_id: str, edms_id: str
    ) -> None:
        """Create a data set update for adding a content copy.

        Returns:
            A DataSetUpdate object.
        """
        self.path = path
        self.commit_id = commit_id
        self.repository_id = repository_id
        self.edms_id = edms_id

        content_copy_actions = self.get_actions_add_content_copy()
        data_set_update = self.get_data_set_update(content_copy_actions)
        self.send_request(data_set_update)

    def delete_content_copy(self, content_copy: "dict[str, Any]") -> None:
        """Deletes the given content_copy from openBIS.

        Args:
            content_copy: Content copy to be deleted.
        """
        content_copy_actions = self.get_actions_remove_content_copy(content_copy)
        data_set_update = self.get_data_set_update(content_copy_actions)
        self.send_request(data_set_update)

    def send_request(self, data_set_update: "dict[str, Any]") -> None:
        """Post the update request to the V3 API."""
        request = {
            "method": "updateDataSets",
            "params": [self.openbis.token, [data_set_update]],
        }
        self.openbis._post_request(self.openbis.as_v3, request)

    def get_data_set_update(
        self, content_copy_actions: "Optional[list[dict[str, Any]]]" = None
    ) -> "dict[str, Any]":
        """Build the DataSetUpdate DTO for the given content-copy actions."""
        return {
            "@type": "as.dto.dataset.update.DataSetUpdate",
            "dataSetId": self.get_data_set_id(),
            "linkedData": self.get_linked_data(
                content_copy_actions if content_copy_actions is not None else []
            ),
        }

    def get_data_set_id(self) -> "dict[str, Any]":
        """Build the DataSetPermId DTO of the updated data set."""
        return {"@type": "as.dto.dataset.id.DataSetPermId", "permId": self.data_set_id}

    def get_linked_data(self, actions: "list[dict[str, Any]]") -> "dict[str, Any]":
        """Build the LinkedDataUpdate DTO wrapping the given actions."""
        return {
            "@type": "as.dto.common.update.FieldUpdateValue",
            "isModified": True,
            "value": {
                "@type": "as.dto.dataset.update.LinkedDataUpdate",
                "contentCopies": {
                    "@type": "as.dto.dataset.update.ContentCopyListUpdateValue",
                    "actions": actions,
                },
            },
        }

    def get_actions_add_content_copy(self) -> "list[dict[str, Any]]":
        """Build the list-update action adding the new content copy."""
        return [
            {
                "@type": "as.dto.common.update.ListUpdateActionAdd",
                "items": [self.get_content_copy_creation()],
            }
        ]

    def get_actions_remove_content_copy(
        self, content_copy: "dict[str, Any]"
    ) -> "list[dict[str, Any]]":
        """Build the list-update action removing the given content copy."""
        return [
            {
                "@type": "as.dto.common.update.ListUpdateActionRemove",
                "items": [content_copy["id"]],
            }
        ]

    def get_content_copy_creation(self) -> "dict[str, Any]":
        """Build the ContentCopyCreation DTO from the stored parameters."""
        return {
            "@type": "as.dto.dataset.create.ContentCopyCreation",
            "externalDmsId": {
                "@type": "as.dto.externaldms.id.ExternalDmsPermId",
                "permId": self.edms_id,
            },
            "path": self.path,
            "gitCommitHash": self.commit_id,
            "gitRepositoryId": self.repository_id,
        }


class GitDataSetFileSearch(object):
    """Searches the files of a link data set on its data store."""

    def __init__(
        self, openbis: Any, data_set_id: str, dss_code: Optional[str] = None
    ) -> None:
        """Initialize the command object with the necessary parameters.

        Args:
            openbis: The openBIS API object.
            data_set_id: Id of the data set to be updated.
            dss_code: Code for the DSS -- defaults to the first dss if none
                is supplied.
        """
        self.openbis = openbis
        self.data_set_id = data_set_id
        self.dss_code = dss_code

    def search_files(self) -> Any:
        """Run the file search on the data store and return the raw response."""
        request = {
            "method": "searchFiles",
            "params": [
                self.openbis.token,
                self.get_data_set_file_search_criteria(),
                self.get_data_set_file_fetch_options(),
            ],
        }
        server_url = self.data_store_url()
        return self.openbis._post_request_full_url(server_url, request)

    def get_data_set_file_search_criteria(self) -> "dict[str, Any]":
        """Build the search criteria matching this data set's code."""
        return {
            "@type": "dss.dto.datasetfile.search.DataSetFileSearchCriteria",
            "operator": "AND",
            "criteria": [
                {
                    "@type": "as.dto.dataset.search.DataSetSearchCriteria",
                    "relation": "DATASET",
                    "operator": "OR",
                    "criteria": [
                        {
                            "fieldName": "code",
                            "fieldType": "ATTRIBUTE",
                            "fieldValue": {
                                "value": self.data_set_id,
                                "@type": "as.dto.common.search.StringEqualToValue",
                            },
                            "@type": "as.dto.common.search.CodeSearchCriteria",
                        }
                    ],
                }
            ],
        }

    def get_data_set_file_fetch_options(self) -> "dict[str, Any]":
        """Build the (empty) file fetch options."""
        return {
            "@type": "dss.dto.datasetfile.fetchoptions.DataSetFileFetchOptions",
        }

    def data_store_url(self) -> str:
        """Return the V3 JSON-RPC endpoint of the target data store."""
        data_stores = self.openbis.get_datastores()
        if self.dss_code is None:
            self.dss_code = self.openbis.get_datastores()["code"][0]
        data_store = data_stores[data_stores["code"] == self.dss_code]
        return f"{data_store['downloadUrl'][0]}/datastore_server/rmi-data-store-server-v3.json"
