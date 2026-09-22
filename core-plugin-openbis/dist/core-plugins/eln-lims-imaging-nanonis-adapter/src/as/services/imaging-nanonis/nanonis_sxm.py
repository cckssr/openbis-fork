#   Copyright ETH 2023-2026 Zürich, Scientific IT Services
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

import numpy
from PIL import Image
import io
import base64
import json
import os
import sys
import skimage

from spmpy import Spm as spm
# from spmpy_terry import spm   # <--- Old library for nanonis data handling.

from nanonis_core import get_sxm_image

import spiepy

import matplotlib.pyplot as plt
# %matplotlib inline

from nanonis_imaging_config import create_sxm_config

method = sys.argv[1]


def find_sxm_file(directory):
    for root, dirs, files1 in os.walk(directory):
        for filename in files1:
            if filename.endswith(".sxm"):
                return os.path.join(root, filename)

    return None

if method.lower() == 'config':
    folder_dir = sys.argv[2]

    file_path = find_sxm_file(folder_dir)

    if file_path:
        print("Found:", file_path)
        images, metadata = create_sxm_config(file_path)

        print(f'{json.dumps(images)}')
        print(f'{json.dumps(metadata)}')
    else:
        raise ValueError("No .sxm file found")


elif method.lower() == 'preview':

    file = sys.argv[2]
    format = sys.argv[3]
    image_config = json.loads(sys.argv[4])
    image_metadata = json.loads(sys.argv[5])
    preview_config = json.loads(sys.argv[6])
    preview_metadata = json.loads(sys.argv[7])
    filter_config = json.loads(sys.argv[8])

    folder_dir = file

    file_path = find_sxm_file(folder_dir)
    if file_path:
        def sxm_mode(sxm_file_path, format, parameters, filter_config, print_out=True):

            colormap_scaling = False
            # 'figure' is default parameter for matplotlib dpi param
            resolution = 'figure'
            filter = "NONE"
            other_params = {}

            print(f"filter_config:{filter_config}")

            for param_key in parameters.keys():

                key = param_key.lower()
                if key == 'channel':
                    channel = parameters[param_key]
                elif key == 'x-axis':
                    x_axis = [float(x) for x in parameters[param_key]]
                elif key == 'y-axis':
                    y_axis = [float(x) for x in parameters[param_key]]
                elif key == 'color-scale':
                    color_scale = [float(x) for x in parameters[param_key]]
                elif key == 'colormap':
                    colormap = parameters[param_key]
                elif key == 'scaling':
                    scaling = parameters[param_key]
                elif key == 'colormap_scaling':
                    colormap_scaling = parameters[param_key].upper() == "TRUE"
                elif key == 'resolution':
                    resolution = parameters[param_key].upper()
                    if resolution == "ORIGINAL":
                        resolution = 'figure'
                    elif resolution.endswith('DPI'):
                        resolution = float(resolution[:-3])
                    else:
                        resolution = float(resolution)
                elif key == 'filter': # TODO remove this once filtering UI is done
                    filter = parameters[param_key].upper() if parameters[param_key] is not None else "NONE"
                else:
                    other_params[param_key] = parameters[param_key]

            preview, img = get_sxm_image(sxm_file_path, format, channel, x_axis, y_axis, scaling, color_scale, colormap,
                                    colormap_scaling, resolution, filter, other_params, filter_config)
            if print_out:
                print(f'{json.dumps(preview)}')

            return img

        sxm_mode(file_path, format, preview_config, filter_config)
    else:
        raise ValueError("No .sxm file found")