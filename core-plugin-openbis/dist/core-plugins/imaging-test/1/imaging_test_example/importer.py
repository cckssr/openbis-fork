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

from pybis import Openbis, ImagingControl, AfsClient
from pybis import *
import pybis.imaging as imaging

TEST_ADAPTOR = "ch.ethz.sis.openbis.generic.server.as.plugins.imaging.adaptor.ImagingTestAdaptor"
VERBOSE = False
DEFAULT_URL = "http://localhost:8888/openbis"
AFS_URL = 'http://localhost:8085/afs-server/api'
# DEFAULT_URL = "https://localhost:8443/openbis"
# DEFAULT_URL = "https://openbis-sis-ci-sprint.ethz.ch/openbis"
# DEFAULT_URL = "https://local.openbis.ch/openbis"


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
afs_url = None

if len(sys.argv) >= 4:
    openbis_url = sys.argv[1]
    data_folder = sys.argv[2]
    afs_url = sys.argv[3]
    if len(sys.argv) > 4:
        token = sys.argv[4]
else:
    print(f'Usage: python3 importer.py <OPENBIS_URL> <PATH_TO_DATA_FOLDER> <AFS_URL>')
    print(f'Using default parameters')
    print(f'URL: {DEFAULT_URL}')
    print(f'Data folder: {data_folder}')
    openbis_url = DEFAULT_URL
    afs_url = AFS_URL

o = get_instance(openbis_url, token)

files = [f for f in os.listdir(data_folder) if f.endswith('.json')]
print(f'Found {len(files)} JSON files in {data_folder}')


client = AfsClient(afs_url, o.token, False)

for file in files:
    file_path = os.path.join(data_folder, file)
    f = open(file_path, 'r')
    data_set = None
    if client.is_session_valid():
        props = {
            'imaging_data_config': f.read(),
            'default_object_view': 'IMAGING_DATASET_VIEWER',
        }
        data_set = o.new_sample('IMAGING_DATA',
                                # experiment='/IMAGING/NANONIS/NANONIS_EXP_1',
                                experiment='/IMAGING/TEST/TEST_COLLECTION',
                                props=props
                                )
        data_set.save()
        text = "hello world!".encode("utf-8")
        client.write(data_set.permId, '/test_file.txt', 0, len(text), text)
    else:
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


# def create_preview(openbis, perm_id, config, preview_format="png", image_index=0, filterConfig=[], tags=[]):
#     imaging_control = ImagingControl(openbis)
#     preview = imaging.ImagingDataSetPreview(preview_format, config=config, filterConfig=filterConfig, tags=tags)
#     preview = imaging_control.make_preview(perm_id, image_index, preview)
#     return preview
#
# config_preview = {
#     # "Channel": channels[0],  # usually one of these: ['z', 'I', 'dIdV', 'dIdV_Y']
#     # "X-axis": ["0", str(img.get_param('width')[0])],  # file dependent
#     # "Y-axis": ["0", str(img.get_param('height')[0])],  # file dependent
#     # "Color-scale": color_scale,  # file dependent
#     "Colormap": "gray",  # [gray, YlOrBr, viridis, cividis, inferno, rainbow, Spectral, RdBu, RdGy]
#     "Scaling": "linear",  # ['linear', 'logarithmic']
# }
#
# create_preview(o, '20251024142437356-46', config_preview)


o.logout()
