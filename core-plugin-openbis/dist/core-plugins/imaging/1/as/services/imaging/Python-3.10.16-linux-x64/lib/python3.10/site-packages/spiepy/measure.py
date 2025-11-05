# -*- coding: utf-8 -*-
#
#   Copyright © 2014 - 2017 Stephan Zevenhuizen
#   SPIEPy, (08-10-2017).
#

from . import locate
from . import Im
import numpy as np
import numpy.matlib
from scipy import ndimage

def _eccentricity(im):
    v = np.array(list(zip(*np.where(im))))
    mu = np.mean(v, 0)
    v_minus_mu = v - np.matlib.repmat(mu, v.shape[0], 1)
    c = np.dot(v_minus_mu.T, v_minus_mu) / v.shape[0]
    eigen = np.linalg.eig(c)
    eccentricity = np.sqrt(1 - eigen[0].min() / eigen[0].max())
    return eccentricity

def feature_properties(im_in, p, thresh_frac, box_mult):
    spiepy_image_structure = type(im_in) == Im
    if spiepy_image_structure:
        im = np.copy(im_in.data)
    else:
        im = np.copy(im_in)
    average_dist, index, dists = locate._average_minimum_distance(p)
    mask_good = np.zeros_like(im, dtype = 'bool')
    mask_all = np.zeros_like(im, dtype = 'bool')
    eccentricity = 0.0
    area = 0.0
    for i in range(len(index)):
        l = int(np.floor(p[1][1][i] - box_mult * dists[i]))
        if l < 0:
            l = 0
        r = int(np.ceil(p[1][1][i] + box_mult * dists[i]))
        if r > im.shape[1] - 1:
            r = im.shape[1] - 1
        cx = p[1][1][i] - l
        b = int(np.floor(p[1][0][i] - box_mult * dists[i]))
        if b < 0:
            b = 0
        t = int(np.ceil(p[1][0][i] + box_mult * dists[i]))
        if t > im.shape[0] - 1:
            t = im.shape[0] - 1
        cy = p[1][0][i] - b
        ref = im[p[0][0][index[i]], p[0][1][index[i]]]
        threshold = ((im[p[1][0][i], p[1][1][i]] - ref) * thresh_frac + ref)
        mask = im[b:t, l:r] > threshold
        mask = ndimage.binary_fill_holes(mask)
        mask_labels, _ = ndimage.label(mask)
        mask = mask_labels == mask_labels[cy, cx]
        edge_mask = np.ones_like(mask, 'bool')
        edge_mask[1:-1, 1:-1] = False
        edge_mask *= mask
        if not edge_mask.any():
            mask_good[b:t, l:r] += mask
        mask_all[b:t, l:r] += mask
    labels, _ = ndimage.label(mask_good)
    feature_labels = [labels[p[1][0][i], p[1][1][i]] for i in range(len(index))]
    feature_labels = [i for i in set(feature_labels)
                      if feature_labels.count(i) == 1]
    count_good = len(feature_labels)
    mask_good *= False
    for i in range(count_good):
        mask = labels == feature_labels[i]
        mask_good += mask
        area += np.sum(mask)
        eccentricity += _eccentricity(mask)
    if count_good:
        eccentricity = eccentricity / count_good
        area = area / count_good
    properties = {'eccentricity': eccentricity,
                  'area'        : area,
                  'count_good'  : count_good,
                  'mask_good'   : mask_good,
                  'mask_all'    : mask_all}
    return properties
