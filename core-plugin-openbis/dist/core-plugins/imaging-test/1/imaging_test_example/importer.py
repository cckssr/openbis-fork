#   Copyright ETH 2023 - 2025 Zürich, Scientific IT Services
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
# Hacky way to import imaging script
import sys
import os

from pybis import Openbis, ImagingControl
from pybis import *

TEST_ADAPTOR = "ch.ethz.sis.openbis.generic.server.dss.plugins.imaging.adaptor.ImagingTestAdaptor"
VERBOSE = False
DEFAULT_URL = "http://localhost:8888/openbis"
# DEFAULT_URL = "https://localhost:8443/openbis"
# DEFAULT_URL = "https://openbis-sis-ci-sprint.ethz.ch/openbis"


def get_instance(url=None, token=None):
    if url is None:
        url = DEFAULT_URL
    openbis_instance = Openbis(
        url=url,
        verify_certificates=False,
        allow_http_but_do_not_use_this_in_production_and_only_within_safe_networks=True
    )
    if token is None:
        token = openbis_instance.login('admin', 'changeit')
    else:
        openbis_instance.token = token
    print(f'Connected to {url} -> token: {token}')
    return openbis_instance


def export_image(openbis: Openbis, perm_id: str, image_id: int, path_to_download: str,
                 include=None, image_format='original', archive_format="zip",
                 resolution='original'):
    if include is None:
        include = ['IMAGE', 'RAW_DATA']
    imaging_control = ImagingControl(openbis)
    export_config = {
        "include": include,
        "image_format": image_format,
        "archive_format": archive_format,
        "resolution": resolution
    }
    imaging_control.export_image(perm_id, image_id, path_to_download, **export_config)


openbis_url = None
data_folder = 'data'
token = None

if len(sys.argv) >= 3:
    openbis_url = sys.argv[1]
    data_folder = sys.argv[2]
    if len(sys.argv) > 3:
        token = sys.argv[3]
else:
    print(f'Usage: python3 importer.py <OPENBIS_URL> <PATH_TO_DATA_FOLDER>')
    print(f'Using default parameters')
    print(f'URL: {DEFAULT_URL}')
    print(f'Data folder: {data_folder}')

o = get_instance(openbis_url, token)

files = [f for f in os.listdir(data_folder) if f.endswith('.json')]
print(f'Found {len(files)} JSON files in {data_folder}')

for file in files:
    file_path = os.path.join(data_folder, file)
    f = open(file_path, 'r')
    props = {
        'imaging_data_config': f.read(),
        'default_dataset_view': 'IMAGING_DATASET_VIEWER',
    }
    data_set = o.new_dataset('IMAGING_DATA',
                             experiment='/IMAGING/TEST/TEST_COLLECTION',
                             sample='/IMAGING/TEST/TEMPLATE-TEST',
                             files=file_path,
                             props=props)
    data_set.save()
    print(f'Created dataset: {data_set.permId}')

# export_image(o, 'permId', 0, 'path_to_download')

o.logout()
