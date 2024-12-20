# -*- coding: utf-8 -*-
#
#   Copyright © 2014 - 2017 Stephan Zevenhuizen
#   SPIEPy, (08-10-2017).
#

from . import locate
from . import Im
import numpy as np
from scipy import ndimage

def by_troughs_and_peaks(im_in, master_mask, mult, sigma):
    spiepy_image_structure = type(im_in) == Im
    if spiepy_image_structure:
        im = np.copy(im_in.data)
    else:
        im = np.copy(im_in)
    master_mask = np.array(master_mask)
    p = locate.troughs_and_peaks(im, sigma)
    if master_mask.shape == im.shape and master_mask.dtype == 'bool':
        p = locate.remove_masked_points(p, master_mask)
    trough_d = np.mean(im[p[0]])
    peak_h = np.mean(im[p[1]])
    p2p = peak_h - trough_d
    mask = ((im < trough_d - p2p * mult[0]) |
            (im > peak_h + p2p * mult[1]))
    p = locate.remove_masked_points(p, mask)
    return mask, p

def by_mean(im_in, master_mask, mult, mode):
    spiepy_image_structure = type(im_in) == Im
    if spiepy_image_structure:
        im = np.copy(im_in.data)
    else:
        im = np.copy(im_in)
    master_mask = np.array(master_mask)
    if master_mask.shape == im.shape and master_mask.dtype == 'bool':
        indices = np.nonzero(~master_mask)
        mean = np.mean(im[indices])
        s = np.std(im[indices])
    else:
        mean = np.mean(im)
        s = np.std(im)
    offset = mult * s
    if not mode in ['a', 'l', 'h']:
        mode = 'a'
        print('Warning: mode changed to \'a\'.')
    if mode == 'a':
        mask = (im > mean + offset) | (im < mean - offset)
    elif mode == 'l':
        mask = im < mean - offset
    else:
        mask = im > mean + offset
    return mask

def tidy_mask(mask, min_size):
    mask = ndimage.binary_fill_holes(mask)
    mask_labels, n = ndimage.label(mask)
    areas = np.bincount(mask_labels.ravel())
    areas[0] = 0
    marked_areas = areas >= min_size
    l_index = [i for i in range(n + 1) if marked_areas[i]]
    mask = mask * False
    for label in l_index:
        mask += mask_labels == label
    return mask
