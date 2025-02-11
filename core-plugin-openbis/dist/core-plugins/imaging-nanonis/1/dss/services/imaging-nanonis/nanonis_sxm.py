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


import spiepy

import matplotlib.pyplot as plt
# %matplotlib inline


class NumpyEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, numpy.ndarray):
            return obj.tolist()
        return json.JSONEncoder.default(self, obj)

def load_image(path):
    return spm(path)


def get_lock_in(img):
    param_name = 'lock-in>lock-in status'
    param = img.get_param(param_name)
    return param


def get_channel(img, channel_name = 'z'):
    channel = img.get_channel(channel_name)
    return channel

print("SYS.ARGV:" + str(sys.argv))
file = sys.argv[1]
format = sys.argv[2]
image_config = json.loads(sys.argv[3])
image_metadata = json.loads(sys.argv[4])
preview_config = json.loads(sys.argv[5])
preview_metadata = json.loads(sys.argv[6])
filter_config = json.loads(sys.argv[7])


params = preview_config

folder_dir = os.path.join(file, 'original')
file_path = os.path.join(folder_dir, os.listdir(folder_dir)[0])


def remove_line_average(chData):
    for i, row in enumerate(chData):
        if not numpy.isnan(row).any():
            try:
                x = numpy.arange(len(row))
                chData[i] -= numpy.polyval(numpy.polyfit(x, row, 1), x)
            except Exception as e:
                print(f"Error at row {i}: {e}\nRow data: {row}")

    return chData


def get_sxm_image(channel_name, x_axis, y_axis, scaling, color_scale, colormap, colormap_scaling,
                  resolution, filter, other_params, filter_config):
    img = load_image(file_path)
    img_byte_arr = io.BytesIO()

    log = False
    if scaling == 'logarithmic':
        log = True
    clim = None

    if filter_config is not None and len(filter_config) > 0:
        (chData,chUnit) = img.get_channel('z', direction = 'forward', flatten=False, offset=False)
        for f in filter_config:
            min_before = numpy.nanmin(chData)
            max_before = numpy.nanmax(chData)
            filter_name = f.upper()
            if filter_name == "GAUSSIAN":
                chData = skimage.filters.gaussian(chData, sigma=int(filter_config[f][0]), truncate=float(filter_config[f][1]))
            elif filter_name == "LAPLACE":
                chData = skimage.filters.laplace(chData, ksize=int(filter_config[f][0]))
            elif filter_name == 'ZERO BACKGROUND':
                chData = chData - min_before
            elif filter_name == 'PLANE SUBTRACTION':
                if ~numpy.isnan(numpy.sum(chData)):
                    chData, _ = spiepy.flatten_xy(chData)
                else:
                    m,n = numpy.shape(chData)
                    i = numpy.argwhere(numpy.isnan(chData))[0,0]
                    im_cut = chData[:i-1,:]
                    chData, _ = spiepy.flatten_xy(im_cut)
                    empty = numpy.full((m-i,n),numpy.nan)
                    chData = numpy.vstack((chData,empty))
            elif filter_name == 'LINE SUBTRACTION':
                chData = remove_line_average(chData)

            range_before = numpy.abs(min_before-max_before)

            x = (color_scale[0]-min_before)/range_before
            y = (color_scale[1]-min_before)/range_before

            min_after = numpy.nanmin(chData)
            max_after = numpy.nanmax(chData)
            range_after = numpy.abs(min_after-max_after)

            color_scale = (x*range_after+min_after, y*range_after+min_after)

    elif filter is not "NONE":
        (chData,chUnit) = img.get_channel('z', direction = 'forward', flatten=False, offset=False)

        min_before = numpy.nanmin(chData)
        max_before = numpy.nanmax(chData)

        if filter == "GAUSSIAN":
            chData = skimage.filters.gaussian(chData, sigma=int(other_params["Gaussian Sigma"][0]), truncate=float(other_params["Gaussian Truncate"][0]))
        elif filter == "LAPLACE":
            chData = skimage.filters.gaussian(chData, sigma=int(other_params["Gaussian Sigma"][0]), truncate=float(other_params["Gaussian Truncate"][0]))
            chData = skimage.filters.laplace(chData, ksize=int(other_params["Laplace Size"][0]))
        elif filter == 'ZERO BACKGROUND':
            chData = chData - min_before
        elif filter == 'PLANE SUBTRACTION':
            if ~numpy.isnan(numpy.sum(chData)):
                chData, _ = spiepy.flatten_xy(chData)
            else:
                m,n = numpy.shape(chData)
                i = numpy.argwhere(numpy.isnan(chData))[0,0]
                im_cut = chData[:i-1,:]
                chData, _ = spiepy.flatten_xy(im_cut)
                empty = numpy.full((m-i,n),numpy.nan)
                chData = numpy.vstack((chData,empty))
        elif filter == 'LINE SUBTRACTION':
            chData = remove_line_average(chData)

        range_before = numpy.abs(min_before-max_before)

        x = (color_scale[0]-min_before)/range_before
        y = (color_scale[1]-min_before)/range_before

        min_after = numpy.nanmin(chData)
        max_after = numpy.nanmax(chData)
        range_after = numpy.abs(min_after-max_after)

        color_scale = (x*range_after+min_after, y*range_after+min_after)

    else:
        (chData,chUnit) = img.get_channel(channel_name, direction ='forward', flatten=False, offset=False)

    my_image = img.plot(show=False, show_params=False, channel=channel_name, log=log, cmap=colormap,
                   color_scale=color_scale, x_axis=x_axis, y_axis=y_axis, colormap_scaling=colormap_scaling, data=(chData,chUnit),
                   clim=clim, axis=False)
    plt.savefig(img_byte_arr, format=format, dpi=resolution)



    fig = plt.figure()
    size = fig.get_size_inches()*fig.dpi

    img_byte_arr = img_byte_arr.getvalue()
    encoded = base64.b64encode(img_byte_arr)
    preview = {'bytes': encoded.decode('utf-8'), 'width': int(size[0]), 'height': int(size[1])}

    # if include_param_info:
    #     print_params = img.print_params_dict(show=False)
    # #     header = json.dumps(img.header, cls=NumpyEncoder)
    # #     preview['header'] = header
    #
    #     # for x in img.header.keys():
    #     #     preview[x] = json.dumps(img.header[x], cls=NumpyEncoder)
    #
    #     for x in print_params.keys():
    #         key = x
    #         if key in ['bytes', 'width', 'height']:
    #             key = 'meta_' + key
    #         preview[key] = print_params[x]
    return preview

def sxm_mode(parameters, filter_config):

    colormap_scaling = False
    include_param_info = False
    # 'figure' is default parameter for matplotlib dpi param
    resolution = 'figure'
    filter = "NONE"
    other_params = {}

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

    preview = get_sxm_image(channel, x_axis, y_axis, scaling, color_scale, colormap,
                            colormap_scaling, resolution, filter, other_params, filter_config)
    print(f'{json.dumps(preview)}')

print(params)
sxm_mode(preview_config, filter_config)