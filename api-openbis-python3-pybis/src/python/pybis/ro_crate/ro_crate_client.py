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

    def export(self, permIds):
        ro_crate_url = self._ro_crate_url + "/export"

        identifiers = permIds

        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Export': 'application/zip',
            'api-key': self.token,
            'openbis.import-compatible': "True",
            'openbis.metadata-pdf': "True",
            'openbis.metadata-xlsx': "True",
            'openbis.dataset-data': "True",
            'openbis.afs-data': "True",
            'openbis.with-levels-above': "True",
            'openbis.with-levels-below': "False",
            'openbis.with-objects-and-dataSets-children': "False",
            'openbis.with-objects-and-dataSets-parents': "False",
            'openbis.with-objects-and-dataSets-other-spaces': "False",
        }

        with self.session.post(ro_crate_url, json.dumps(identifiers), headers=headers, verify=self._verify) as response:
            if response.ok:
                response_obj = response.json()
                job_id = response_obj["jobId"]
                return job_id
            else:
                parsed_error = json.loads(response.text)
                raise ValueError(f"{parsed_error}")

    def check_status(self, job_id):
        ro_crate_url = self._ro_crate_url + "/status"
        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'api-key': self.token,
            'jobId': job_id
        }

        with self.session.get(ro_crate_url, headers=headers, verify=self._verify) as response:
            if response.ok:
                response_json = response.json()
                if response_json['status'] == 'FAILED':
                    print(response_json["errors"])
                    raise ValueError(f"Something failed: {response_json['errors']}")
                content_type = response.headers['Content-Type']
                if content_type != 'application/json':
                    content = response.content
                    # with open('/tmp/out.zip', 'wb') as out_file:
                    #     out_file.write(response.content)
                    return "TODO"
                else:
                    status = response_json['status']
                    return status
                return response.text
            else:
                parsed_error = json.loads(response.text)
                # message = parsed_error['error'][1]['message']
                raise ValueError(f"{parsed_error}")



