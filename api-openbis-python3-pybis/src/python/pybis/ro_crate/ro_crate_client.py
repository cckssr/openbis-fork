#   Copyright ETH 2026 Zürich, Scientific IT Services
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
import requests
import json
from requests.adapters import HTTPAdapter, Retry
from typing import Union


REQUEST_RETRIES_COUNT = 5

def _create_session(url):
    """Create a session object to handle retries in case of server failure"""
    session = requests.Session()
    retries = Retry(total=REQUEST_RETRIES_COUNT, backoff_factor=1,
                    status_forcelist=[502, 503, 504])
    session.mount(url, HTTPAdapter(max_retries=retries))
    return session

class RoCrateClient:

    def __init__(self, url, token, verify=True):
        self._ro_crate_url = url
        if url is not None and not url.endswith("/open-api/ro-crate"):
            self._ro_crate_url = url + "/open-api/ro-crate"
        self.token = token
        self._verify = verify
        self.session = _create_session(url)

    def echo(self, message):
        ro_crate_url = self._ro_crate_url + "/test-echo"
        params = {
            "message": message
        }

        with self.session.get(ro_crate_url, params=params, verify=self._verify) as response:
            if response.ok:
                return response.text
            else:
                parsed_error = json.loads(response.text)
                raise ValueError(f"{parsed_error}")

    def test_connection(self):
        ro_crate_url = self._ro_crate_url + "/test-openbis-connection"
        params = {
            "api-key": self.token
        }
        with self.session.get(ro_crate_url, params=params, verify=self._verify) as response:
            if response.ok:
                return response.text
            else:
                parsed_error = json.loads(response.text)
                raise ValueError(f"{parsed_error}")

    def export(self, permIds: list, zipExport:bool=True, withLevelsBelow:bool=False):
        ro_crate_url = self._ro_crate_url + "/export"

        identifiers = []

        for permId in permIds:
            if isinstance(permId, str):
                identifiers.append({"kind":"SAMPLE", "permId":permId})
            else:
                identifiers.append(permId)


        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Export': 'application/zip' if zipExport is True else 'application/ld+json',
            'api-key': self.token,
            'openbis.import-compatible': "True",
            'openbis.metadata-pdf': "True",
            'openbis.metadata-xlsx': "True",
            'openbis.dataset-data': "True",
            'openbis.afs-data': "True",
            'openbis.with-levels-above': "True",
            'openbis.with-levels-below': str(withLevelsBelow),
            'openbis.with-objects-and-dataSets-children': "False",
            'openbis.with-objects-and-dataSets-parents': "False",
            'openbis.with-objects-and-dataSets-other-spaces': "False",
            'openbis.input-body-format': 'json'
        }

        with self.session.post(ro_crate_url, json.dumps(identifiers), headers=headers, verify=self._verify) as response:
            if response.ok:
                response_obj = response.json()
                job_id = response_obj["jobId"]
                return job_id
            else:
                response.raise_for_status()
                raise ValueError(f"{response.text}")

    def check_status(self, job_id):
        if job_id is None:
            raise ValueError("job_id cannot be None")
        ro_crate_url = self._ro_crate_url + "/status/" + job_id
        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'api-key': self.token,
            'jobId': job_id
        }

        with self.session.get(ro_crate_url, headers=headers, verify=self._verify) as response:
            if response.ok:
                response_json = response.json()
                job = response_json
                return Status(job)
            else:
                response.raise_for_status()
                raise ValueError(f"{response.text}")

    def download(self, job_id, destination, zip=True):
        if job_id is None:
            raise ValueError("job_id cannot be None")
        if destination is None:
            raise ValueError("destination cannot be None")

        ro_crate_url = self._ro_crate_url + "/download"
        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'api-key': self.token,
            'jobId': job_id
        }

        if zip:
            file_path = destination + "/ro_crate_metadata" + job_id + ".zip"
        else:
            file_path = destination + "/ro_crate" + job_id + ".json"

        with self.session.get(ro_crate_url, headers=headers, verify=self._verify, stream=True) as response:
            if response.ok:
                with open(file_path, "wb") as f:
                    for chunk in response.iter_content(chunk_size=1024*1024):
                        f.write(chunk)
                return file_path
            else:
                response.raise_for_status()
                raise ValueError(f"{response.text}")

    def check_statuses(self):
        ro_crate_url = self._ro_crate_url + "/status"
        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'api-key': self.token,
        }

        with self.session.get(ro_crate_url, headers=headers, verify=self._verify) as response:
            if response.ok:
                response_json = response.json()
                return [Status(job) for job in response_json['jobs']]
            else:
                response.raise_for_status()
                raise ValueError(f"{response.text}")

class ValidationReport:
    isValid: bool
    entities: list
    errors: list

    def __init__(self, jsonResponse):
        self.isValid = jsonResponse['isValid']
        self.entities = jsonResponse['entities']
        self.errors = jsonResponse['errors']

    def __str__(self):
        return f'ValidationReport[{self.isValid},{self.entities},{self.errors}]'

    def __repr__(self):
        return self.__str__()

class ImportResponse:
    externalToOpenBisIdentifier: dict

    def __init__(self, externalToOpenBisIdentifier):
        self.externalToOpenBisIdentifier = externalToOpenBisIdentifier

    def __str__(self):
        return f'ImportResponse[{self.externalToOpenBisIdentifier}]'

    def __repr__(self):
        return self.__str__()

class Status:
    jobId: str
    status: str
    errors: Union[list, None]
    downloadUrl: Union[str, None]
    validationResult: Union[ValidationReport, None]
    importResponse: Union[ImportResponse, None]

    def __init__(self, jsonResponse):
        self.jobId = jsonResponse['jobId']
        self.status = jsonResponse['status']
        self.errors = jsonResponse['errors']
        self.downloadUrl = jsonResponse['downloadUrl']
        self.validationResult = ValidationReport(jsonResponse['validationResult']) if jsonResponse['validationResult'] is not None else None
        self.importResponse = ImportResponse(jsonResponse['importResponse']) if jsonResponse['importResponse'] is not None else None


    def __str__(self):
        return f'Status[{self.jobId},{self.status}]'

    def __repr__(self):
        return self.__str__()