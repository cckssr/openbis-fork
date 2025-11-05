# -*- coding: utf-8 -*-
#
#   Copyright © 2014 Stephan Zevenhuizen
#   SPIEPy, (05-12-2014).
#

import numpy as np
from scipy import ndimage
from . import Im

def _average_minimum_distance(p):
    n = len(p[1][0])
    dists = np.empty(n)
    index = np.empty(n, 'int')
    for i in range(n):
        rel_pos = ((1.0 * p[0][1] - p[1][1][i]) ** 2 +
                   (1.0 * p[0][0] - p[1][0][i]) ** 2)
        minpos = np.argmin(rel_pos)
        min_i = rel_pos[minpos]
        dists[i] = np.sqrt(min_i)
        index[i] = minpos
    average_dist = np.mean(dists)
    return average_dist, index, dists

def remove_masked_points(p, mask):
    a0 = np.zeros_like(mask, 'bool')
    a1 = np.zeros_like(mask, 'bool')
    a0[p[0]] = True
    a1[p[1]] = True
    a0 = a0 * ~mask
    a1 = a1 * ~mask
    p = [np.nonzero(a0)]
    p.append(np.nonzero(a1))
    return p

def troughs_and_peaks(im_in, sigma):
    spiepy_image_structure = type(im_in) == Im
    if spiepy_image_structure:
        im = np.copy(im_in.data)
    else:
        im = np.copy(im_in)
    im_blur = ndimage.gaussian_filter(im, sigma, mode = 'nearest')
    a0 = ndimage.maximum_filter(-im_blur, 8) == -im_blur
    a1 = ndimage.maximum_filter(im_blur, 8) == im_blur
    edge_mask = np.zeros_like(im, 'bool')
    edge_mask[1:-1, 1:-1] = True
    a0 = a0 * edge_mask
    a1 = a1 * edge_mask
    p = [np.nonzero(a0)]
    p.append(np.nonzero(a1))
    return p

def regions(im_in, sigma, mask):
    spiepy_image_structure = type(im_in) == Im
    if spiepy_image_structure:
        im = np.copy(im_in.data)
    else:
        im = np.copy(im_in)
    mask = np.array(mask)
    masked = im.shape == mask.shape and mask.dtype == 'bool'
    p = troughs_and_peaks(im, sigma)
    if masked:
        peaks = remove_masked_points(p, mask)[1]
    else:
        peaks = p[1]
    values = im[peaks]
    shape = im.shape
    im = []
    for y in range(shape[0]):
        for x in range(shape[1]):
            rel_pos = ((1.0 * peaks[1] - x) ** 2 +
                       (1.0 * peaks[0] - y) ** 2)
            im.append(values[np.argmin(rel_pos)])
    im = np.array(im).reshape(shape)
    if spiepy_image_structure:
        im_out = im_in.copy()
        im_out.data = im
    else:
        im_out = im
    return im_out, peaks

def find_steps(im_in, sigma, low_threshold, high_threshold):
    spiepy_image_structure = type(im_in) == Im
    if spiepy_image_structure:
        im = np.copy(im_in.data)
    else:
        im = np.copy(im_in)
    im_min = im.min()
    im_max = im.max()
    im = (im - im_min) / (im_max - im_min)
    im = ndimage.gaussian_filter(im, sigma, mode = 'nearest')
    grad_x = ndimage.sobel(im, 1)
    grad_y = ndimage.sobel(im, 0)
    abs_grad_x = np.absolute(grad_x)
    abs_grad_y = np.absolute(grad_y)
    grad = np.hypot(grad_x, grad_y)
    eroded_mask = np.zeros_like(im, 'bool')
    eroded_mask[1:-1, 1:-1] = True
    local_maxima = np.zeros_like(im)
    
    pts_plus = (grad_x > 0) & (grad_y >= 0) & (abs_grad_x > abs_grad_y)
    pts_minus = (grad_x < 0) & (grad_y <= 0) & (abs_grad_x > abs_grad_y)
    pts = eroded_mask & (pts_plus | pts_minus)
    g = grad[pts]
    w = abs_grad_y[pts] / abs_grad_x[pts]
    c1 = grad[:, 1:][pts[:, :-1]]
    c2 = grad[1:, 1:][pts[:-1, :-1]]
    ca = c1 * (1 - w) + c2 * w <= g
    c1 = grad[:, :-1][pts[:, 1:]]
    c2 = grad[:-1, :-1][pts[1:, 1:]]
    cb = c1 * (1 - w) + c2 * w <= g
    local_maxima[pts] = g * ca * cb

    pts_plus = (grad_x > 0) & (grad_y > 0) & (abs_grad_x <= abs_grad_y)
    pts_minus = (grad_x < 0) & (grad_y < 0) & (abs_grad_x <= abs_grad_y)
    pts = eroded_mask & (pts_plus | pts_minus)
    g = grad[pts]
    w = abs_grad_x[pts] / abs_grad_y[pts]
    c1 = grad[1:, :][pts[:-1, :]]
    c2 = grad[1:, 1:][pts[:-1, :-1]]
    ca = c1 * (1 - w) + c2 * w <= g
    c1 = grad[:-1, :][pts[1:, :]]
    c2 = grad[:-1, :-1][pts[1:, 1:]]
    cb = c1 * (1 - w) + c2 * w <= g
    local_maxima[pts] = g * ca * cb

    pts_plus = (grad_x <= 0) & (grad_y > 0) & (abs_grad_x < abs_grad_y)
    pts_minus = (grad_x >= 0) & (grad_y < 0) & (abs_grad_x < abs_grad_y)
    pts = eroded_mask & (pts_plus | pts_minus)
    g = grad[pts]
    w = abs_grad_x[pts] / abs_grad_y[pts]
    c1 = grad[1:, :][pts[:-1, :]]
    c2 = grad[1:, :-1][pts[:-1, 1:]]
    ca = c1 * (1 - w) + c2 * w <= g
    c1 = grad[:-1, :][pts[1:, :]]
    c2 = grad[:-1, 1:][pts[1:, :-1]]
    cb = c1 * (1 - w) + c2 * w <= g
    local_maxima[pts] = g * ca * cb

    pts_plus = (grad_x < 0) & (grad_y > 0) & (abs_grad_x >= abs_grad_y)
    pts_minus = (grad_x > 0) & (grad_y < 0) & (abs_grad_x >= abs_grad_y)
    pts = eroded_mask & (pts_plus | pts_minus)
    g = grad[pts]
    w = abs_grad_y[pts] / abs_grad_x[pts]
    c1 = grad[:, :-1][pts[:, 1:]]
    c2 = grad[1:, :-1][pts[:-1, 1:]]
    ca = c1 * (1 - w) + c2 * w <= g
    c1 = grad[:, 1:][pts[:, :-1]]
    c2 = grad[:-1, 1:][pts[1:, :-1]]
    cb = c1 * (1 - w) + c2 * w <= g
    local_maxima[pts] = g * ca * cb
    
    high_mask = local_maxima > high_threshold
    low_mask = local_maxima > low_threshold
    s = np.ones((3, 3), 'bool')
    labels, n = ndimage.label(low_mask, s)
    if n:
        sums = ndimage.sum(high_mask, labels, np.arange(n) + 1)
        good_label = np.zeros((n + 1, ), 'bool')
        good_label[1:] = sums > 0
        mask = good_label[labels]
    else:
        mask = low_mask
    return mask
