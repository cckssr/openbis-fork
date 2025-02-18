#   Copyright ETH 2025 Zürich, Scientific IT Services
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
import skimage

from spmpy import Spm as spm

# from spmpy import Spm as spm # <---new library does not work well with dat

from spmpy_terry import spm   # <--- class spm defines objects of type spm with their attributes and class functions
import spmpy_terry as spmpy   # <--- spmpy has other methods
from datetime import datetime

import matplotlib.pyplot as plt
# %matplotlib inline

# JSON deserializer
class NumpyEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, numpy.ndarray):
            return obj.tolist()
        return json.JSONEncoder.default(self, obj)

# Nanonis utils
def load_image(path):
    return spm(path)

def get_lock_in(img):
    param_name = 'lock-in>lock-in status'
    param = img.get_param(param_name)
    return param


def get_channel(img, channel_name = 'z'):
    channel = img.get_channel(channel_name)
    return channel

def get_upper_case_dict(params):
    output = {}
    for k,v in params.items():
        output[k.upper()] = v
    return output

# Filters
def remove_line_average(chData):
    for i, row in enumerate(chData):
        if not numpy.isnan(row).any():
            try:
                x = numpy.arange(len(row))
                chData[i] -= numpy.polyval(numpy.polyfit(x, row, 1), x)
            except Exception as e:
                print(f"Error at row {i}: {e}\nRow data: {row}")

    return chData


# DAT image generation
def get_dat_image(folder_dir, format, channel_x, channel_y, x_axis, y_axis, colormap, scaling, grouping, print_legend, resolution):
    specs = spmpy.importall(folder_dir, '', 'spec')

    for spec in specs:
        date_time = spec.get_param('Saved Date')
        spec.date_time = datetime.strptime(date_time, "%d.%m.%Y %H:%M:%S") if date_time is not None else datetime.now()
        # spec.date_time = datetime.strptime(date_time, "%d.%m.%Y %H:%M:%S")

    # sort measurements according to date
    specs.sort(key=lambda d: d.date_time)
    specs_sub = list(filter(lambda spec:spec.name in grouping, specs))

    print_legend = print_legend
    show = False
    fig = spmpy.specs_plot(specs_sub, channelx=channel_x, channely=channel_y, direction='forward',
                           print_legend=print_legend, show=show, colormap=colormap, scaling=scaling,
                           x_axis=x_axis, y_axis=y_axis)
    img_byte_arr = io.BytesIO()
    plt.savefig(img_byte_arr, format=format, dpi=resolution)

    fig = plt.figure()
    size = fig.get_size_inches()*fig.dpi

    img_byte_arr = img_byte_arr.getvalue()
    encoded = base64.b64encode(img_byte_arr)
    preview = {'bytes': encoded.decode('utf-8'), 'width': int(size[0]), 'height': int(size[1])}

    # if include_param_info:
    #     # print_params = img.print_params_dict(show=False)
    #     # #     header = json.dumps(img.header, cls=NumpyEncoder)
    #     # #     preview['header'] = header
    #     #
    #     # # for x in img.header.keys():
    #     # #     preview[x] = json.dumps(img.header[x], cls=NumpyEncoder)
    #     #
    #     # for x in print_params.keys():
    #     #     key = x
    #     #     if key in ['bytes', 'width', 'height']:
    #     #         key = 'meta_' + key
    #     #     preview[key] = print_params[x]

    return preview

def get_sxm_image(sxm_file_path, format, channel_name, x_axis, y_axis, scaling, color_scale, colormap, colormap_scaling,
                  resolution, filter, other_params, filter_config, print_out=True):
    img = load_image(sxm_file_path)
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
            filter_name = list(f.keys())[0]
            filter_parameters = get_upper_case_dict(f[filter_name])
            if filter_name.upper() == "GAUSSIAN":
                sigma = int(filter_parameters['SIGMA'])
                truncate = float(filter_parameters['TRUNCATE'])
                chData = skimage.filters.gaussian(chData, sigma=sigma, truncate=truncate)
            elif filter_name.upper() == "LAPLACE":
                size = int(filter_parameters["SIZE"])
                chData = skimage.filters.laplace(chData, ksize=size)
            elif filter_name.upper() == 'ZERO BACKGROUND':
                chData = chData - min_before
            elif filter_name.upper() == 'PLANE SUBTRACTION':
                if ~numpy.isnan(numpy.sum(chData)):
                    chData, _ = spiepy.flatten_xy(chData)
                else:
                    m,n = numpy.shape(chData)
                    i = numpy.argwhere(numpy.isnan(chData))[0,0]
                    im_cut = chData[:i-1,:]
                    chData, _ = spiepy.flatten_xy(im_cut)
                    empty = numpy.full((m-i,n),numpy.nan)
                    chData = numpy.vstack((chData,empty))
            elif filter_name.upper() == 'LINE SUBTRACTION':
                chData = remove_line_average(chData)

            range_before = numpy.abs(min_before-max_before)

            x = (color_scale[0]-min_before)/range_before
            y = (color_scale[1]-min_before)/range_before

            min_after = numpy.nanmin(chData)
            max_after = numpy.nanmax(chData)
            range_after = numpy.abs(min_after-max_after)

            color_scale = (x*range_after+min_after, y*range_after+min_after)

    elif filter != "NONE":
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

    # img.plot(show=False, show_params=False, channel=channel_name, log=log, cmap=colormap,
    #          color_scale=color_scale, x_axis=x_axis, y_axis=y_axis, colormap_scaling=colormap_scaling, data=(chData,chUnit),
    #          clim=clim, axis=False)

    img.plot(show=False, show_params=False, channel=channel_name, log=log, cmap=colormap,
             color_scale=color_scale, colormap_scaling=colormap_scaling, data=(chData,chUnit),
             clim=clim, axis=False)

    if print_out:
        plt.savefig(img_byte_arr, format=format, dpi=resolution)

        fig = plt.figure()
        size = fig.get_size_inches()*fig.dpi

        img_byte_arr = img_byte_arr.getvalue()
        encoded = base64.b64encode(img_byte_arr)
        preview = {'bytes': encoded.decode('utf-8'), 'width': int(size[0]), 'height': int(size[1])}
    else:
        preview = None
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
    return preview, img