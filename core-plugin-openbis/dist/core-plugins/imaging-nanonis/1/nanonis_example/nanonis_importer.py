#   Copyright ETH 2023 - 2024 Zürich, Scientific IT Services
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
import sys
import os
import copy

from pybis import Openbis, ImagingControl
import pybis.imaging as imaging

import numpy as np

from spmpy import Spm as spm

from datetime import datetime
import shutil
from collections import defaultdict

SXM_ADAPTOR = "ch.ethz.sis.openbis.generic.server.dss.plugins.imaging.adaptor.NanonisSxmAdaptor"
DAT_ADAPTOR = "ch.ethz.sis.openbis.generic.server.dss.plugins.imaging.adaptor.NanonisDatAdaptor"
VERBOSE = False
DEFAULT_URL = "http://localhost:8888/openbis"
# DEFAULT_URL = "http://local.openbis.ch:8080/openbis"
# DEFAULT_URL = "https://openbis-sis-ci-sprint.ethz.ch/openbis"


def get_instance(url=None):
    if url is None:
        url = DEFAULT_URL
    openbis_instance = Openbis(
        url=url,
        verify_certificates=False,
        allow_http_but_do_not_use_this_in_production_and_only_within_safe_networks=True
    )
    token = openbis_instance.login('admin', 'changeit')
    print(f'Connected to {url} -> token: {token}')
    return openbis_instance


def get_color_scale_range(img, channel):
    minimum = np.nanmin(img.get_channel(channel)[0])
    maximum = np.nanmax(img.get_channel(channel)[0])

    step = abs(round((maximum - minimum) / 100, 2))
    if step >= 1:
        step = 1
    elif step > 0:
        step = 0.01
    else:
        step = abs((maximum - minimum) / 100)
        step = np.log10(step)
        if np.isnan(step) or np.isinf(step):
            step = 0.01
        else:
            step = 10 ** np.floor(step)

    return [str(minimum), str(maximum), str(step)]

def reorder_sxm_channels(channels, header):
    """
    Lock-in>Lock-in status: ON > dIdV vs V
    Lock-in>Lock-in status: OFF:
        Z-Ctrl hold: TRUE > z vs V
        Z-Ctrl hold: FALSE:
            Oscillation Control>output off: TRUE > df vs V
            Oscillation Control>output off: FALSE > I vs V
    """
    channel_index = -1
    if header["lock-in>lock-in status"] == "ON":
        channel_index = channels.index("dIdV")
    else:
        if header["z-controller>controller status"] == "ON":
            channel_index = channels.index("z")
        else:
            if header["oscillation control>output off"] == "TRUE":
                channel_index = channels.index("df")
            else:
                channel_index = channels.index("I")

    # If the channel index is less than 0, it means the tree did not find the measurement type
    if channel_index >= 0:
        channels[channel_index], channels[0] = channels[0], channels[channel_index]

    return channels


def create_sxm_dataset(openbis, experiment, file_path, sample=None):
    img = spm(file_path)
    channels = [x['ChannelNickname'] for x in img.SignalsList]
    header = img.header

    # Select default channel according to the measurement type
    channels = reorder_sxm_channels(channels, header)

    imaging_control = ImagingControl(openbis)

    color_scale_visibility = [
        imaging.ImagingDataSetControlVisibility(
            "Channel",
            [channel],
            get_color_scale_range(img, channel),
            img.get_channel(channel)[1])
        for channel in channels]

    exports = [imaging.ImagingDataSetControl('include', "Dropdown", values=['image', 'raw data'], multiselect=True),
               imaging.ImagingDataSetControl('image-format', "Dropdown", values=['png', 'svg']),
               imaging.ImagingDataSetControl('archive-format', "Dropdown", values=['zip', 'tar']),
               imaging.ImagingDataSetControl('resolution', "Dropdown", values=['original', '150dpi', '300dpi'])]

    inputs = [
        imaging.ImagingDataSetControl('Channel', "Dropdown", values=channels, section="Data"),
        imaging.ImagingDataSetControl('X-axis', "Range", section="Data", values_range=["0", str(img.get_param('width')[0]), "0.01"]),
        imaging.ImagingDataSetControl('Y-axis', "Range", section="Data", values_range=["0", str(img.get_param('height')[0]), "0.01"]),
        imaging.ImagingDataSetControl('Color-scale', "Range", section="Data", visibility=color_scale_visibility),
        imaging.ImagingDataSetControl('Scaling', "Dropdown", section="Data", values=['linear', 'logarithmic']),
        imaging.ImagingDataSetControl('Colormap', "Colormap", values=['gray', 'YlOrBr', 'viridis', 'cividis', 'inferno', 'rainbow', 'Spectral', 'RdBu', 'RdGy']),

        # TODO Remove below params once filter UI is done
        imaging.ImagingDataSetControl('Filter', "Dropdown", section="Filter", values=['None', 'Gaussian', 'Laplace', 'Zero background', 'Plane Subtraction', 'Line Subtraction']),
        imaging.ImagingDataSetControl('Gaussian Sigma', "Slider", section="Filter", visibility=[
            imaging.ImagingDataSetControlVisibility('Filter', ['None', 'Zero background', 'Plane Subtraction', 'Line Subtraction'], ['1', '1', '1']),
            imaging.ImagingDataSetControlVisibility('Filter', ['Gaussian', 'Laplace'], ['1', '100', '1'])
        ]),
        imaging.ImagingDataSetControl('Gaussian Truncate', "Slider",  section="Filter", visibility=[
            imaging.ImagingDataSetControlVisibility('Filter', ['None', 'Zero background', 'Plane Subtraction', 'Line Subtraction'], ['1', '1', '0.1']),
            imaging.ImagingDataSetControlVisibility('Filter', ['Gaussian', 'Laplace'], ['0', '1', '0.1'])
        ]),
        imaging.ImagingDataSetControl('Laplace Size', "Slider",  section="Filter", visibility=[
            imaging.ImagingDataSetControlVisibility('Filter', ['None', 'Gaussian', 'Zero background', 'Plane Subtraction', 'Line Subtraction'], ['1', '1', '1']),
            imaging.ImagingDataSetControlVisibility('Filter', ['Laplace'], ['3', '30', '1']),
        ]),
        # TODO END of removal part

    ]

    filters = {
        'Gaussian': [imaging.ImagingDataSetControl('Sigma', "Slider", section="Gaussian", values_range=['1', '100', '1']),
                     imaging.ImagingDataSetControl('Truncate', "Slider", section="Gaussian", values_range=['0', '1', '0.1'])],
        'Laplace': [imaging.ImagingDataSetControl('Size', "Slider", section="Laplace", values_range=['3', '30', '1'])],
        'Zero background': [],
        'Plane Subtraction': [],
        'Line Subtraction': []
    }

    imaging_config = imaging.ImagingDataSetConfig(
        adaptor=SXM_ADAPTOR,
        version=1.0,
        resolutions=['original', '200x200', '2000x2000'],
        playable=True,
        speeds=[1000, 2000, 5000],
        exports=exports,
        inputs=inputs,
        metadata={},
        filters=filters)

    images = [imaging.ImagingDataSetImage(imaging_config,
                                          previews=[imaging.ImagingDataSetPreview(preview_format="png")],
                                          metadata=img.print_params_dict(False)
                                          )]
    imaging_property_config = imaging.ImagingDataSetPropertyConfig(images)
    if VERBOSE:
        print(imaging_property_config.to_json())

    return imaging_control.create_imaging_dataset(
        dataset_type="IMAGING_DATA",
        config=imaging_property_config,
        experiment=experiment,
        sample=sample,
        files=[file_path],
    )

def reorder_dat_channels(channels, header):
    """
    Bias spectroscopy:
        Lock-in>Lock-in status: ON > dIdV vs V
        Lock-in>Lock-in status: OFF
            Z-Ctrl hold: TRUE > z vs V
            Z-Ctrl hold: FALSE
                Oscillation Control>output off: TRUE > df vs V
                Oscillation Control>output off: FALSE > I vs V

    Z spectroscopy:
        Lock-in>Lock-in status: ON > dIdV vs z
        Lock-in>Lock-in status: OFF
            Oscillation Control>output off: TRUE > df vs z
            Oscillation Control>output off: FALSE > I vs z
    """
    channels_x = copy.deepcopy(channels)
    channels_y = copy.deepcopy(channels)
    channel_x_index = -1
    channel_y_index = -1

    if {} == header:
        return channels_x, channels_y

    def get_header_value(text):
        if text in header:
            return header[text]
        return None

    if get_header_value("Experiment") == "bias spectroscopy":
        if get_header_value("Lock-in>Lock-in status") == "ON":
            channel_x_index = channels_x.index(("V","V",1))
            channel_y_index = channels_y.index(("dIdV","pA",10**12))
        else:
            if get_header_value("Z-Ctrl hold") == "FALSE":
                channel_x_index = channels_x.index(("V","V",1))
                channel_y_index = channels_y.index(("zspec","nm",10**9))
            else:
                if get_header_value("Oscillation Control>output off") == "TRUE":
                    channel_x_index = channels_x.index(("V","V",1))
                    channel_y_index = channels_y.index(("df","Hz",1))
                else:
                    channel_x_index = channels_x.index(("V","V",1))
                    channel_y_index = channels_y.index(("I","pA",10**12))
    else:
        if get_header_value("Lock-in>Lock-in status") == "ON":
            channel_x_index = channels_x.index(("zspec","nm",10**9))
            channel_y_index = channels_y.index(("dIdV","pA",10**12))
        else:
            if get_header_value("Oscillation Control>output off") == "TRUE":
                channel_x_index = channels_x.index(("zspec","nm",10**9))
                channel_y_index = channels_y.index(("df","Hz",1))
            else:
                channel_x_index = channels_x.index(("zspec","nm",10**9))
                channel_y_index = channels_y.index(("I","pA",10**12))

    if channel_x_index >= 0:
        channels_x[channel_x_index], channels_x[0] = channels_x[0], channels_x[channel_x_index]
        channels_y[channel_y_index], channels_y[0] = channels_y[0], channels_y[channel_y_index]
    return channels_x, channels_y

def get_dat_type(header):
    """
    Bias spectroscopy:
        Lock-in>Lock-in status: ON > dIdV vs V
        Lock-in>Lock-in status: OFF
            Z-Ctrl hold: TRUE > z vs V
            Z-Ctrl hold: FALSE
                Oscillation Control>output off: TRUE > df vs V
                Oscillation Control>output off: FALSE > I vs V

    Z spectroscopy:
        Lock-in>Lock-in status: ON > dIdV vs z
        Lock-in>Lock-in status: OFF
            Oscillation Control>output off: TRUE > df vs z
            Oscillation Control>output off: FALSE > I vs z
    """
    if {} == header:
        return "bias spectroscopy z vs V"

    def get_header_value(text):
        if text in header:
            return header[text]
        return None

    measurement_type = ""

    if get_header_value("Experiment") == "bias spectroscopy":
        if get_header_value("Lock-in>Lock-in status") == "ON":
            measurement_type = "bias spectroscopy dIdV vs V"
        else:
            if get_header_value("Z-Ctrl hold") == "TRUE":
                measurement_type = "bias spectroscopy z vs V"
            else:
                if get_header_value("Oscillation Control>output off") == "TRUE":
                    measurement_type = "bias spectroscopy df vs V"
                else:
                    measurement_type = "bias spectroscopy I vs V"
    else:
        if get_header_value("Lock-in>Lock-in status") == "ON":
            measurement_type = "z spectroscopy dIdV vs z"
        else:
            if get_header_value("Oscillation Control>output off") == "TRUE":
                measurement_type = "z spectroscopy df vs z"
            else:
                measurement_type = "z spectroscopy I vs z"

    return measurement_type

def _min_max_step(channel, data):
    minimum, maximum = [], []
    for spec in data:
        minimum += [np.nanmin(spec.get_channel(f'{channel}')[0])]
        maximum += [np.nanmax(spec.get_channel(f'{channel}')[0])]
    minimum = np.nanmin(minimum)
    maximum = np.nanmax(maximum)

    step = abs(round((maximum - minimum) / 100, 2))

    if step >= 1:
        step = 1
    elif step > 0:
        step = 0.01
    else:
        step = abs((maximum - minimum) / 100)
        step = np.log10(step)
        if np.isnan(step) or np.isinf(step):
            step = 0.01
        else:
            step = 10 ** np.floor(step)
    return [str(minimum), str(maximum), str(step)]


def create_dat_dataset(openbis, folder_path, file_prefix='', sample=None, experiment=None):
    assert experiment is not None or sample is not None, "Either sample or experiment needs to be provided!"
    data = spm.importall(folder_path, file_prefix, 'spec')
    if [] == data:
        raise ValueError(f"No nanonis .DAT files found in {folder_path}")

    imaging_control = ImagingControl(openbis)

    for d in data:
        if d.type == 'scan':
            date = d.get_param('rec_date')
            time = d.get_param('rec_time')
            date_time = '%s %s' % (date, time)
            d.date_time = datetime.strptime(date_time, "%d.%m.%Y %H:%M:%S")

        if d.type == 'spec':
            date_time = d.get_param('Saved Date')
            d.date_time = datetime.strptime(date_time, "%d.%m.%Y %H:%M:%S") if date_time is not None else datetime.now()

    data.sort(key=lambda da: da.date_time)
    channels = list(set([(channel['ChannelNickname'], channel['ChannelUnit'], channel['ChannelScaling']) for spec in data for channel in spec.SignalsList]))
    channels_x, channels_y = reorder_dat_channels(channels, data[0].header) # All files inside data belong to the same measurement type. Thus, the header of the first file can be used for all of them.


    color_scale_visibility_x = []
    color_scale_visibility_y = []
    for idx, (channel_x, unit_x, scaling_x) in enumerate(channels_x):
        channel_y = channels_y[idx][0]
        unit_y = channels_y[idx][1]
        scaling_y = channels_y[idx][2]

        (minimum_x, maximum_x, step_x) = _min_max_step(channel_x, data)
        (minimum_y, maximum_y, step_y) = _min_max_step(channel_y, data)

        color_scale_visibility_x += [imaging.ImagingDataSetControlVisibility(
            "Channel X",
            [channel_x],
            [str(minimum_x), str(maximum_x), str(step_x)],
            unit_x
        )]

        color_scale_visibility_y += [imaging.ImagingDataSetControlVisibility(
            "Channel Y",
            [channel_y],
            [str(minimum_y), str(maximum_y), str(step_y)],
            unit_y
        )]

    exports = [imaging.ImagingDataSetControl('include', "Dropdown", values=['image', 'raw data'], multiselect=True),
               imaging.ImagingDataSetControl('image-format', "Dropdown", values=['png', 'svg']),
               imaging.ImagingDataSetControl('archive-format', "Dropdown", values=['zip', 'tar']),
               imaging.ImagingDataSetControl('resolution', "Dropdown", values=['original', '150dpi', '300dpi'])]

    inputs = [
        imaging.ImagingDataSetControl('Channel X', "Dropdown", values=[channel[0] for channel in channels_x]),
        imaging.ImagingDataSetControl('Channel Y', "Dropdown", values=[channel[0] for channel in channels_y]),
        imaging.ImagingDataSetControl('X-axis', "Range", visibility=color_scale_visibility_x),
        imaging.ImagingDataSetControl('Y-axis', "Range", visibility=color_scale_visibility_y),
        imaging.ImagingDataSetControl('Grouping', "Dropdown", values=[d.name for d in data], multiselect=True),
        imaging.ImagingDataSetControl('Colormap', "Colormap", values=['gray', 'YlOrBr', 'viridis', 'cividis', 'inferno', 'rainbow', 'Spectral', 'RdBu', 'RdGy']),
        imaging.ImagingDataSetControl('Scaling', "Dropdown", values=['lin-lin', 'lin-log', 'log-lin', 'log-log']),
        imaging.ImagingDataSetControl('Include parameter information', "Dropdown", values=['True', 'False']),
        # imaging.ImagingDataSetControl('Print legend', "Dropdown", values=['True', 'False']),
    ]

    imaging_config = imaging.ImagingDataSetConfig(
        DAT_ADAPTOR,
        1.0,
        ['original', '200x200', '2000x2000'],
        True,
        [1000, 2000, 5000],
        exports,
        inputs,
        {})

    images = [imaging.ImagingDataSetImage(imaging_config)]
    imaging_property_config = imaging.ImagingDataSetPropertyConfig(images)
    if VERBOSE:
        print(imaging_property_config.to_json())

    return imaging_control.create_imaging_dataset(
        dataset_type="IMAGING_DATA",
        config=imaging_property_config,
        experiment=experiment,
        sample=sample,
        files=[d.path for d in data])


def create_preview(openbis, perm_id, config, preview_format="png", image_index=0, filterConfig=[]):
    imaging_control = ImagingControl(openbis)
    preview = imaging.ImagingDataSetPreview(preview_format, config=config, filterConfig=filterConfig)
    preview = imaging_control.make_preview(perm_id, image_index, preview)
    return preview


def update_image_with_preview(openbis, perm_id, image_id, preview: imaging.ImagingDataSetPreview):
    imaging_control = ImagingControl(openbis)
    config = imaging_control.get_property_config(perm_id)
    image = config.images[image_id]
    if len(image.previews) > preview.index:
        image.previews[preview.index] = preview
    else:
        preview.index = len(image.previews)
        image.add_preview(preview)

    imaging_control.update_property_config(perm_id, config)


def export_image(openbis: Openbis, perm_id: str, image_id: int, path_to_download: str,
                 include=None, image_format='original', archive_format="zip", resolution='original'):
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


def multi_export_images(openbis: Openbis, perm_ids: list[str], image_ids: list[int], preview_ids: list[int],
                        path_to_download: str, include=None, image_format='original',
                        archive_format="zip", resolution='original'):
    if include is None:
        include = ['IMAGE', 'RAW_DATA']
    imaging_control = ImagingControl(openbis)
    export_config = {
        "include": include,
        "image_format": image_format,
        "archive_format": archive_format,
        "resolution": resolution
    }

    imaging_control.export_previews(perm_ids, image_ids, preview_ids, path_to_download, **export_config)


def demo_sxm_flow(openbis, file_sxm, experiment=None, sample=None, permId=None):

    perm_id = permId
    if experiment is None:
        experiment = '/IMAGING/NANONIS/SXM_COLLECTION'
    if sample is None:
        sample = '/IMAGING/NANONIS/TEMPLATE-SXM'
    if perm_id is None:
        dataset_sxm = create_sxm_dataset(
            openbis=openbis,
            experiment=experiment,
            sample=sample,
            file_path=file_sxm)
        perm_id = dataset_sxm.permId
        print(f'Created imaging .SXM dataset: {dataset_sxm.permId}')

    print(f'Computing preview for dataset: {perm_id}')
    img = spm(file_path)
    channels = [x['ChannelNickname'] for x in img.SignalsList]
    header = img.header

    # Select default channel according to the measurement type
    channels = reorder_sxm_channels(channels, header)

    color_scale = get_color_scale_range(img, channels[0])[:2]

    config_sxm_preview = {
        "Channel": channels[0],  # usually one of these: ['z', 'I', 'dIdV', 'dIdV_Y']
        "X-axis": ["0", str(img.get_param('width')[0])],  # file dependent
        "Y-axis": ["0", str(img.get_param('height')[0])],  # file dependent
        "Color-scale": color_scale,  # file dependent
        "Colormap": "gray",  # [gray, YlOrBr, viridis, cividis, inferno, rainbow, Spectral, RdBu, RdGy]
        "Scaling": "linear",  # ['linear', 'logarithmic']
        "Filter": "None",
        "Gaussian Sigma": "0",
        "Gaussian Truncate": "0",
        "Laplace Size": "3"
    }
    config_preview = config_sxm_preview.copy()

    preview = create_preview(openbis, perm_id, config_preview)

    preview.index = 0
    update_image_with_preview(openbis, perm_id, 0, preview)

    config_preview = config_sxm_preview.copy()
    config_preview['Scaling'] = 'logarithmic'

    filter_config = [
        {"Gaussian": ["20", "0.3"] },
        {"Laplace": ["3"] }
    ]

    preview = create_preview(openbis, perm_id, config_preview, filterConfig=filter_config)
    preview.index = 1
    update_image_with_preview(openbis, perm_id, 0, preview)

    config_preview = config_sxm_preview.copy()
    config_preview['Colormap'] = 'inferno'

    filter_config = [
        {"Gaussian": ["20", "0.3"] },
        {"Zero background": [] }
    ]

    preview = create_preview(openbis, perm_id, config_preview, filterConfig=filter_config)
    preview.index = 2
    update_image_with_preview(openbis, perm_id, 0, preview)


def demo_dat_flow(openbis, folder_path, permId=None):

    print(f'Searching for .DAT files')
    perm_id = permId
    if permId is None:
        dataset_dat = create_dat_dataset(
            openbis=openbis,
            experiment='/IMAGING/NANONIS/SXM_COLLECTION',
            sample='/IMAGING/NANONIS/TEMPLATE-DAT',
            folder_path=folder_path,
            file_prefix='didv_')
        perm_id = dataset_dat.permId
        print(f'Created imaging .DAT dataset: {dataset_dat.permId}')

    print(f'Computing previews for dataset: {perm_id}')

    data = spm.importall(folder_path, '', 'spec')

    for d in data:
        if d.type == 'scan':
            date = d.get_param('rec_date')
            time = d.get_param('rec_time')
            date_time = '%s %s' % (date, time)
            d.date_time = datetime.strptime(date_time, "%d.%m.%Y %H:%M:%S")

        if d.type == 'spec':
            date_time = d.get_param('Saved Date')
            d.date_time = datetime.strptime(date_time, "%d.%m.%Y %H:%M:%S") if date_time is not None else datetime.now()

    data.sort(key=lambda da: da.date_time)
    channels = list(set([(channel['ChannelNickname'], channel['ChannelUnit'], channel['ChannelScaling']) for spec in data for channel in spec.SignalsList]))
    channels_x, channels_y = reorder_dat_channels(channels, data[0].header) # All files inside data belong to the same measurement type. Thus, the header of the first file can be used for all of them.

    channel_x = channels_x[0][0]
    channel_y = channels_y[0][0]

    if channel_x == channel_y:
        channel_x = 'V'
        channel_y = 'dIdV'


    (minimum_x, maximum_x, step_x) = _min_max_step(channel_x, data)
    (minimum_y, maximum_y, step_y) = _min_max_step(channel_y, data)

    config_dat_preview = {
        "Channel X": channel_x,
        "Channel Y": channel_y,
        "X-axis": [str(minimum_x), str(maximum_x)],
        "Y-axis": [str(minimum_y), str(maximum_y)],
        "Grouping": sorted([os.path.basename(str(f)) for f in data]),
        "Colormap": "rainbow",
        "Scaling": "lin-lin",  # ['lin-lin', 'lin-log', 'log-lin', 'log-log']
        "Print legend": "True", # disable legend in image
    }

    config_preview = config_dat_preview.copy()

    preview = create_preview(openbis, perm_id, config_preview)

    preview.index = 0
    update_image_with_preview(openbis, perm_id, 0, preview)

    config_preview = config_dat_preview.copy()
    config_preview["Scaling"] = 'log-log'
    preview = create_preview(openbis, perm_id, config_preview)
    preview.index = 1
    update_image_with_preview(openbis, perm_id, 0, preview)


openbis_url = None
data_folder = 'data'

if len(sys.argv) > 2:
    openbis_url = sys.argv[1]
    data_folder = sys.argv[2]
else:
    print(f'Usage: python3 nanonis_importer.py <OPENBIS_URL> <PATH_TO_DATA_FOLDER>')
    print(f'Using default parameters')
    print(f'URL: {DEFAULT_URL}')
    print(f'Data folder: {data_folder}')

o = get_instance(openbis_url)

measurement_files = [f for f in os.listdir(data_folder)]
measurement_datetimes = []

for f in measurement_files:
    if f.endswith(".sxm"):
        img = spm(f"{data_folder}/{f}")
        img_datetime = datetime.strptime(f"{img.header['rec_date']} {img.header['rec_time']}", "%d.%m.%Y %H:%M:%S")
        measurement_datetimes.append(img_datetime)

    elif f.endswith(".dat"):
        img = spm(f"{data_folder}/{f}")
        img_datetime = datetime.now()
        if 'Saved Date' in img.header:
            img_datetime = datetime.strptime(img.header['Saved Date'], "%d.%m.%Y %H:%M:%S")
        measurement_datetimes.append(img_datetime)

# Sort files by datetime
sorted_measurement_files = [x for _, x in sorted(zip(measurement_datetimes, measurement_files))]

# Dat files belonging to the same measurement session, i.e., that are consecutive, should be grouped into just one list of files.
grouped_measurement_files = []
group = []
for i,f in enumerate(sorted_measurement_files):
    if f.endswith(".sxm"):
        if len(group) > 0:
            grouped_measurement_files.append(group)
            group = []
        grouped_measurement_files.append([f])
    elif f.endswith(".dat"):
        group.append(f)

if len(group) > 0:
    grouped_measurement_files.append(group)
    group = []


for group in grouped_measurement_files:
    if group[0].endswith(".sxm"):
        print(f"SXM file: {group[0]}")
        file_path = os.path.join(data_folder, group[0])
        try:
            demo_sxm_flow(o, file_path)
            # exit(0)
        except ValueError as e:
            print(f"Cannot upload {group[0]}. Reason: {e}")
    else:

        # Split the dat files by measurement type (e.g.: bias spec dI vs V in one list, bias spec z vs V in another list, etc.)
        dat_files_types = []
        for dat_file in group:
            dat_data = spm(f"{data_folder}/{dat_file}")
            dat_files_types.append(get_dat_type(dat_data.header))

        grouped = defaultdict(list)

        for item1, item2 in zip(group, dat_files_types):
            grouped[item2].append(item1)

        dat_files_grouped_by_type = list(grouped.values())
        # ---------------

        for dat_files_group in dat_files_grouped_by_type:
            dat_files_directory = os.path.join(data_folder, "dat_files")
            shutil.rmtree(dat_files_directory, ignore_errors=True)
            os.mkdir(dat_files_directory)

            for dat_file in dat_files_group:
                shutil.copy(os.path.join(data_folder, dat_file), os.path.join(dat_files_directory, dat_file))
            try:
                demo_dat_flow(o, dat_files_directory)
            except ValueError as e:
                print(f"Cannot upload {dat_files_directory}. Reason: {e}")
            shutil.rmtree(dat_files_directory)

# export_image(o, '20241218074117986-44', 0, '/home/alaskowski/PREMISE/test')
# export_image(o, '20240111135043750-39', 0, '/home/alaskowski/PREMISE')

# multi_export_images(o, ['20241218074117986-44', '20241218074117986-44'],
#                     [0, 0],
#                     [0, 1],
#                     '/home/alaskowski/PREMISE/test2')

o.logout()
