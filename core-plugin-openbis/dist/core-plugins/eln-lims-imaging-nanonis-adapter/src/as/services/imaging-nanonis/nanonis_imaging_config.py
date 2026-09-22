from pybis import (ImagingDataSetConfig, ImagingDataSetImage, ImagingDataSetPreview,
                   ImagingDataSetPropertyConfig, ImagingDataSetControl, ImagingDataSetControlVisibility,
                   ImagingSemanticAnnotation)
from spmpy import Spm as spm
import numpy as np
from datetime import datetime
import copy
from collections import defaultdict
import itertools

SXM_ADAPTOR = "ch.ethz.sis.openbis.generic.server.as.plugins.imaging.adaptor.NanonisSxmAdaptor"
DAT_ADAPTOR = "ch.ethz.sis.openbis.generic.server.as.plugins.imaging.adaptor.NanonisDatAdaptor"


###################################
####   SXM config generation   ####
###################################


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
        if step == 0:
            print(f'[WARNING] During to generate config: min[{minimum}] and max[{maximum}] values for channel[{channel}] are too close!')
            return [str(minimum), str(maximum), str(0.0001)]
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

    lock_in_status = -1
    z_controller_status = -1
    oscillation_control_output_off = -1

    if "lock-in>lock-in status" in header:
        if header["lock-in>lock-in status"] == "ON":
            lock_in_status = 1
        else:
            lock_in_status = 0

    if "z-controller>controller status" in header:
        if header["z-controller>controller status"] == "ON":
            z_controller_status = 1
        else:
            z_controller_status = 0

    if "oscillation control>output off" in header:
        if header["oscillation control>output off"] == "TRUE":
            oscillation_control_output_off = 1
        else:
            oscillation_control_output_off = 0

    try:
        if lock_in_status == 1:
            channel_index = channels.index("dIdV")
        elif lock_in_status == 0:
            if z_controller_status == 1:
                channel_index = channels.index("z")
            else:
                if oscillation_control_output_off == 1:
                    channel_index = channels.index("df")
                else:
                    channel_index = channels.index("I")
        else:
            if "z" in channels:
                channel_index = channels.index("z")
            else:
                channel_index = 0 # Select first channel available
    except:
        channel_index = 0

    # If the channel index is less than 0, it means the tree did not find the measurement type
    if channel_index >= 0:
        channels[channel_index], channels[0] = channels[0], channels[channel_index]

    return channels

def create_sxm_config(sxm_file_path):

    img = spm(sxm_file_path)
    # channels = [x['ChannelNickname'] for x in img.SignalsList]
    channels = [x for x in img.signals]
    # channels = [img.signals[x]['ChannelName'] for x in img.signals]
    header = img.header

    # Select default channel according to the measurement type
    channels = reorder_sxm_channels(channels, header)


    color_scale_visibility = [
        ImagingDataSetControlVisibility(
            "Channel",
            [channel],
            get_color_scale_range(img, channel),
            img.get_channel(channel)[1])
        for channel in channels]

    exports = [ImagingDataSetControl('include', "Dropdown", values=['image', 'raw data'], multiselect=True),
               ImagingDataSetControl('image-format', "Dropdown", values=['png', 'svg'], semanticAnnotation=ImagingSemanticAnnotation('schema.org', 'https://schema.org/version/28.1', 'https://schema.org/encoding')),
               ImagingDataSetControl('archive-format', "Dropdown", values=['zip', 'tar'], semanticAnnotation=ImagingSemanticAnnotation('schema.org', 'https://schema.org/version/28.1', 'https://schema.org/fileFormat')),
               ImagingDataSetControl('resolution', "Dropdown", values=['original', '150dpi', '300dpi'], semanticAnnotation=None),
               ImagingDataSetControl('include labels', "Dropdown", values=['True', 'False'], semanticAnnotation=None),
               ImagingDataSetControl('include parameters', "Dropdown", values=['True', 'False'], semanticAnnotation=None)]

    inputs = [
        ImagingDataSetControl('Channel', "Dropdown", values=channels, section="Data"),
        ImagingDataSetControl('X-axis', "Range", section="Data", values_range=["0", str(img.get_param('width')[0]), "0.01"]),
        ImagingDataSetControl('Y-axis', "Range", section="Data", values_range=["0", str(img.get_param('height')[0]), "0.01"]),
        ImagingDataSetControl('Color-scale', "Range", section="Data", visibility=color_scale_visibility),
        ImagingDataSetControl('Scaling', "Dropdown", section="Data", values=['linear', 'logarithmic']),
        ImagingDataSetControl('Colormap', "Colormap", values=['gray', 'YlOrBr', 'viridis', 'cividis', 'inferno', 'rainbow', 'Spectral', 'RdBu', 'RdGy'], semanticAnnotation=ImagingSemanticAnnotation('schema.org', 'https://schema.org/version/28.1', 'https://schema.org/color')),

        ImagingDataSetControl('include labels', "Dropdown", values=['True', 'False'], semanticAnnotation=None),
        ImagingDataSetControl('include parameters', "Dropdown", values=['True', 'False'], semanticAnnotation=None)
    ]

    filters = {
        'Gaussian': [ImagingDataSetControl('Sigma', "Slider", section="Gaussian", values_range=['1', '100', '1']),
                     ImagingDataSetControl('Truncate', "Slider", section="Gaussian", values_range=['0', '1', '0.1'])],
        'Laplace': [ImagingDataSetControl('Size', "Slider", section="Laplace", values_range=['3', '30', '1'])],
        'Zero background': [],
        'Plane Subtraction': [],
        'Line Subtraction': []
    }

    filterSemanticAnnotation = {
        'Gaussian': ImagingSemanticAnnotation('schema.org', 'https://schema.org/version/28.1', 'https://schema.org/headline')
    }

    imaging_config = ImagingDataSetConfig(
        adaptor=SXM_ADAPTOR,
        version=1.0,
        resolutions=['original', '200x200', '2000x2000'],
        playable=True,
        speeds=[1000, 2000, 5000],
        exports=exports,
        inputs=inputs,
        metadata={},
        filters=filters,
        filterSemanticAnnotation=filterSemanticAnnotation)

    images = [ImagingDataSetImage(imaging_config,
                                          previews=[ImagingDataSetPreview(preview_format="png")],
                                          metadata=img.print_params_dict(False)
                                          )]

    images = [x.to_json() for x in images]

    return images, {'note': 'server-generated config'}




###################################
####   DAT config generation   ####
###################################

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

    lock_in_status = -1
    z_control_hold = -1
    oscillation_control_output_off = -1

    if "Lock-in>Lock-in status" in header:
        if header["Lock-in>Lock-in status"] == "ON":
            lock_in_status = 1
        else:
            lock_in_status = 0

    if "Z-Ctrl hold" in header:
        if header["Z-Ctrl hold"] == "TRUE":
            z_control_hold = 1
        else:
            z_control_hold = 0

    if "Oscillation Control>output off" in header:
        if header["Oscillation Control>output off"] == "TRUE":
            oscillation_control_output_off = 1
        else:
            oscillation_control_output_off = 0

    try:
        if header["Experiment"] == "bias spectroscopy":
            try:
                if lock_in_status == 1:
                    channel_x_index = channels_x.index(("V","V",1))
                    channel_y_index = channels_y.index(("dIdV","pS",10**12))
                elif lock_in_status == 0:
                    if z_control_hold == 0:
                        channel_x_index = channels_x.index(("V","V",1))
                        channel_y_index = channels_y.index(("zspec","nm",10**9))
                    else:
                        if oscillation_control_output_off == 1:
                            channel_x_index = channels_x.index(("V","V",1))
                            channel_y_index = channels_y.index(("df","Hz",1))
                        else:
                            channel_x_index = channels_x.index(("V","V",1))
                            channel_y_index = channels_y.index(("I","pA",10**12))
                else:
                    channel_x_index = channels_x.index(("V","V",1))
                    channel_y_index = 1
            except:
                channel_x_index = channels_x.index(("V","V",1))
                channel_y_index = 1
        else:
            try:
                if lock_in_status == 1:
                    channel_x_index = channels_x.index(("zspec","nm",10**9))
                    channel_y_index = channels_y.index(("dIdV","pS",10**12))
                elif lock_in_status == 0:
                    if oscillation_control_output_off == 1:
                        channel_x_index = channels_x.index(("zspec","nm",10**9))
                        channel_y_index = channels_y.index(("df","Hz",1))
                    else:
                        channel_x_index = channels_x.index(("zspec","nm",10**9))
                        channel_y_index = channels_y.index(("I","pA",10**12))
                else:
                    channel_x_index = channels_x.index(("zspec","nm",10**9))
                    channel_y_index = 1
            except:
                channel_x_index = channels_x.index(("zspec","nm",10**9))
                channel_y_index = 1
    except:
        channel_x_index = 0
        channel_y_index = 1

    if channel_x_index >= 0:
        channels_x[channel_x_index], channels_x[0] = channels_x[0], channels_x[channel_x_index]
        channels_y[channel_y_index], channels_y[0] = channels_y[0], channels_y[channel_y_index]
    return channels_x, channels_y


def create_dat_config(dat_folder_path):
    data = spm.importall(dat_folder_path, 'spec')
    if [] == data:
        raise ValueError(f"No nanonis .DAT files found in {dat_folder_path}")

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
    channels = list([list([channel, spec.signals[channel]['ChannelUnit'], spec.signals[channel]['ChannelScaling']]) for spec in data for channel in spec.signals])
    channels.sort()
    channels = list(k for k,_ in itertools.groupby(channels))
    channels_x, channels_y = reorder_dat_channels(channels, data[0].header) # All files inside data belong to the same measurement type. Thus, the header of the first file can be used for all of them.


    color_scale_visibility_x = []
    color_scale_visibility_y = []
    for idx, (channel_x, unit_x, scaling_x) in enumerate(channels_x):
        channel_y = channels_y[idx][0]
        unit_y = channels_y[idx][1]
        scaling_y = channels_y[idx][2]

        minimum_x, maximum_x = [], []
        minimum_y, maximum_y = [], []


        for spec in data:
            # # -------- Boolean flag was added to the code -------
            # channel_in_signals_list = False
            # for signal_settings in spec.SignalsList:
            #     if channel_x in signal_settings["ChannelNickname"]:
            #         channel_in_signals_list = True
            # if channel_in_signals_list:
            # # ---------------------------------------------------
            minimum_x += [np.nanmin(spec.get_channel(f'{channel_x}')[0])]
            maximum_x += [np.nanmax(spec.get_channel(f'{channel_x}')[0])]

            minimum_y += [np.nanmin(spec.get_channel(f'{channel_y}')[0])]
            maximum_y += [np.nanmax(spec.get_channel(f'{channel_y}')[0])]
        minimum_x = np.nanmin(minimum_x)
        maximum_x = np.nanmax(maximum_x)

        minimum_y = np.nanmin(minimum_y)
        maximum_y = np.nanmax(maximum_y)
        step_x = abs(round((maximum_x - minimum_x) / 100, 2))
        step_y = abs(round((maximum_y - minimum_y) / 100, 2))
        if step_x >= 1000:
            step_x = 10 ** np.floor(np.log10(step_x))
        elif step_x >= 1:
            step_x = 1
        elif step_x > 0:
            step_x = 0.01
        else:
            step_x = abs((maximum_x - minimum_x) / 100)
            if step_x == 0:
                step_x = 0.0001
            else:
                step_x = np.log10(step_x)
                if np.isnan(step_x) or np.isinf(step_x):
                    step_x = 0.01
                else:
                    step_x = 10 ** np.floor(step_x)

        if step_y >= 1000:
            step_y = 10 ** np.floor(np.log10(step_y))
        elif step_y >= 1:
            step_y = 1
        elif step_y > 0:
            step_y = 0.01
        else:
            step_y = abs((maximum_y - minimum_y) / 100)
            if step_y == 0:
                step_y = 0.0001
            else:
                step_y = np.log10(step_y)
                if np.isnan(step_y) or np.isinf(step_y):
                    step_y = 0.01
                else:
                    step_y = 10 ** np.floor(step_y)

        color_scale_visibility_x += [ImagingDataSetControlVisibility(
            "Channel X",
            [channel_x],
            [str(minimum_x), str(maximum_x), str(step_x)],
            unit_x
        )]

        color_scale_visibility_y += [ImagingDataSetControlVisibility(
            "Channel Y",
            [channel_y],
            [str(minimum_y), str(maximum_y), str(step_y)],
            unit_y
        )]

    exports = [ImagingDataSetControl('include', "Dropdown", values=['image', 'raw data'], multiselect=True),
               ImagingDataSetControl('image-format', "Dropdown", values=['png', 'svg'], semanticAnnotation=ImagingSemanticAnnotation('schema.org', 'https://schema.org/version/28.1', 'https://schema.org/encoding')),
               ImagingDataSetControl('archive-format', "Dropdown", values=['zip', 'tar'], semanticAnnotation=ImagingSemanticAnnotation('schema.org', 'https://schema.org/version/28.1', 'https://schema.org/fileFormat')),
               ImagingDataSetControl('resolution', "Dropdown", values=['original', '150dpi', '300dpi'])]

    inputs = [
        ImagingDataSetControl('Channel X', "Dropdown", values=[channel[0] for channel in channels_x]),
        ImagingDataSetControl('Channel Y', "Dropdown", values=[channel[0] for channel in channels_y]),
        ImagingDataSetControl('X-axis', "Range", visibility=color_scale_visibility_x),
        ImagingDataSetControl('Y-axis', "Range", visibility=color_scale_visibility_y),
        ImagingDataSetControl('Grouping', "Dropdown", values=[d.name for d in data], multiselect=True),
        ImagingDataSetControl('Colormap', "Colormap", values=['gray', 'YlOrBr', 'viridis', 'cividis', 'inferno', 'rainbow', 'Spectral', 'RdBu', 'RdGy']),
        ImagingDataSetControl('Scaling', "Dropdown", values=['lin-lin', 'lin-log', 'log-lin', 'log-log']),
        # imaging.ImagingDataSetControl('Print legend', "Dropdown", values=['True', 'False']),
    ]

    imaging_config = ImagingDataSetConfig(
        DAT_ADAPTOR,
        1.0,
        ['original', '200x200', '2000x2000'],
        True,
        [1000, 2000, 5000],
        exports,
        inputs,
        {})

    # images = [imaging.ImagingDataSetImage(imaging_config)]
    # imaging_property_config = imaging.ImagingDataSetPropertyConfig(images)

    images = [ImagingDataSetImage(imaging_config,
                                  previews=[ImagingDataSetPreview(preview_format="png")],
                                  # metadata=img.print_params_dict(False)
                                  )]

    images = [x.to_json() for x in images]


    return images, {"note": "server-generated config"}