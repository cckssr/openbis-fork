#   Copyright ETH 2023 Zürich, Scientific IT Services
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

import numpy as np
from PIL import Image
import io
import base64
import sys
import json
import os
import sys

from nanonis_core import get_dat_image, get_sxm_image

# from spmpy import Spm as spm # <---new library does not work well with dat

from spmpy_terry import spm   # <--- class spm defines objects of type spm with their attributes and class functions
import spmpy_terry as spmpy   # <--- spmpy has other methods
from datetime import datetime

import matplotlib.pyplot as plt
import matplotlib.pylab as pl
import matplotlib.patches as mpatches
# %matplotlib inline


# def load_image(path):
#     return spm(path)
#
#
# def get_lock_in(img):
#     param_name = 'lock-in>lock-in status'
#     param = img.get_param(param_name)
#     return param
#
#
# def get_channel(img, channel_name = 'z'):
#     # channel_name = 'z'
#     channel = img.get_channel(channel_name)
#     return channel


file = sys.argv[1]
format = sys.argv[2]
image_config = json.loads(sys.argv[3])
image_metadata = json.loads(sys.argv[4])
preview_config = json.loads(sys.argv[5])
preview_metadata = json.loads(sys.argv[6])
filter_config = json.loads(sys.argv[7])



folder_dir = os.path.join(file, 'original')
file_path = os.path.join(folder_dir, os.listdir(folder_dir)[0])
# print(file_path)


# def get_dat_image(channel_x, channel_y, x_axis, y_axis, colormap, scaling, grouping, print_legend, resolution, include_param_info):
#     specs = spmpy.importall(folder_dir, '', 'spec')
#
#     for spec in specs:
#         date_time = spec.get_param('Saved Date')
#         spec.date_time = datetime.strptime(date_time, "%d.%m.%Y %H:%M:%S") if date_time is not None else datetime.now()
#         # spec.date_time = datetime.strptime(date_time, "%d.%m.%Y %H:%M:%S")
#
#     # sort measurements according to date
#     specs.sort(key=lambda d: d.date_time)
#     specs_sub = list(filter(lambda spec:spec.name in grouping, specs))
#
#     print_legend = print_legend
#     show = False
#     fig = spmpy.specs_plot(specs_sub, channelx=channel_x, channely=channel_y, direction='forward',
#                            print_legend=print_legend, show=show, colormap=colormap, scaling=scaling,
#                            x_axis=x_axis, y_axis=y_axis)
#     img_byte_arr = io.BytesIO()
#     plt.savefig(img_byte_arr, format=format, dpi=resolution)
#
#     fig = plt.figure()
#     size = fig.get_size_inches()*fig.dpi
#
#     img_byte_arr = img_byte_arr.getvalue()
#     encoded = base64.b64encode(img_byte_arr)
#     preview = {'bytes': encoded.decode('utf-8'), 'width': int(size[0]), 'height': int(size[1])}
#
#     # if include_param_info:
#     #     # print_params = img.print_params_dict(show=False)
#     #     # #     header = json.dumps(img.header, cls=NumpyEncoder)
#     #     # #     preview['header'] = header
#     #     #
#     #     # # for x in img.header.keys():
#     #     # #     preview[x] = json.dumps(img.header[x], cls=NumpyEncoder)
#     #     #
#     #     # for x in print_params.keys():
#     #     #     key = x
#     #     #     if key in ['bytes', 'width', 'height']:
#     #     #         key = 'meta_' + key
#     #     #     preview[key] = print_params[x]
#
#     return preview


def dat_mode(parameters):

    colormap_scaling = False
    # 'figure' is default parameter for matplotlib dpi param
    resolution = 'figure'
    color = False
    print_legend = True

    for param_key in parameters.keys():

        key = param_key.lower()
        if key == 'channel x':
            channel_x = parameters[param_key]
        elif key == 'channel y':
            channel_y = parameters[param_key]
        elif key == 'x-axis':
            x_axis = [float(x) for x in parameters[param_key]]
        elif key == 'y-axis':
            y_axis = [float(x) for x in parameters[param_key]]
        elif key == 'grouping':
            grouping = parameters[param_key]
        elif key == 'colormap':
            colormap = parameters[param_key]
        elif key == 'scaling':
            scaling = parameters[param_key]
        elif key == 'color':
            color = parameters[param_key]
        elif key == 'color':
            color = parameters[param_key]
        elif key == 'print legend':
            print_legend = parameters[param_key].upper() == "TRUE"
        elif key == 'resolution':
            resolution = parameters[param_key].upper()
            if resolution == "ORIGINAL":
                resolution = 'figure'
            elif resolution.endswith('DPI'):
                resolution = float(resolution[:-3])
            else:
                resolution = float(resolution)


    input_config = dict(
        format=format,
        folder_dir=folder_dir,
        channel_x=channel_x,
        channel_y=channel_y,
        x_axis=x_axis,
        y_axis=y_axis,
        colormap=colormap,
        scaling=scaling,
        grouping=grouping
    )
    if color:
        input_config['color'] = color

    input_config['print_legend'] = print_legend
    input_config['resolution'] = resolution


    # width, height, image_bytes = get_dat_image(**input_config)
    preview = get_dat_image(**input_config)
    print(f'{json.dumps(preview)}')


def sxm_mode(sxm_file_path, format, parameters, filter_config, print_out=True):

    colormap_scaling = False
    # 'figure' is default parameter for matplotlib dpi param
    resolution = 'figure'
    filter = "NONE"
    other_params = {}

    print(f"KEYS:{list(parameters.keys())}")

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
                                 colormap_scaling, resolution, filter, other_params, filter_config, print_out)
    if print_out:
        print(f'{json.dumps(preview)}')

    return img



params = preview_config

if 'spectraLocator' in params and params['spectraLocator'].upper() == "TRUE":

    sxmConfig = params['sxmPreviewConfig']
    root_folder_path = params['sxmRootPath']
    file_path = params['sxmFilePath']
    sxm_path = os.path.join(root_folder_path, file_path)

    img = sxm_mode(sxm_path, format, sxmConfig, {}, print_out=False)

    specs = spmpy.importall(folder_dir, '', 'spec')
    if 'Grouping' in params and params['Grouping'] is not None:
        grouping = params['Grouping']
    else:
        grouping = None

    resolution = 'figure'
    if 'resolution' in params:
        resolution = params['resolution'].upper()
        if resolution == "ORIGINAL":
            resolution = 'figure'
        elif resolution.endswith('DPI'):
            resolution = float(resolution[:-3])
        else:
            resolution = float(resolution)

    specs_sub = list(filter(lambda spec:spec.name in grouping, specs)) if grouping is not None else specs
    specs_sub.sort(key=lambda x: x.name)

    col = pl.cm.rainbow(np.linspace(0,1,len(specs_sub)))

    ref_img = img
    legend = []
    # plot circle for each location
    for (s,c) in zip(specs_sub,col):
        (x,y) = spmpy.relative_position(ref_img,s)
        patch = mpatches.Patch(color=c, label=s.name)
        legend += [patch]
        plt.plot(x,y,'ro',color = c)

    fig = plt.gcf()
    fig_width, fig_height = fig.get_size_inches()

    # Stupid, but works
    fig.set_figwidth(fig_width*1.5)
    ax = fig.add_subplot(1, 4, 1)
    ax.axis('off')
    ax.legend(handles=legend, loc='best')

    # fig.legend(handles=legend, loc='outside left center')

    img_byte_arr = io.BytesIO()
    plt.savefig(img_byte_arr, format=format, dpi=resolution, bbox_inches="tight")

    fig = plt.figure()
    size = fig.get_size_inches()*fig.dpi

    img_byte_arr = img_byte_arr.getvalue()
    encoded = base64.b64encode(img_byte_arr)
    preview = {'bytes': encoded.decode('utf-8'), 'width': int(size[0]), 'height': int(size[1])}
    print(f'{json.dumps(preview)}')

else:
    dat_mode(preview_config)
