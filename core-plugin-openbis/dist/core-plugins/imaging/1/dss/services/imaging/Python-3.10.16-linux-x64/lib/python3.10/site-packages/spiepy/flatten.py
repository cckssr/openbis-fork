# -*- coding: utf-8 -*-
#
#   Copyright © 2014 - 2023 Stephan Zevenhuizen
#   SPIEPy, (24-11-2023).
#

import itertools
import numpy as np
from . import Im
from . import locate
from . import mask as _mask

def _cells(mask):
    indices = np.nonzero(~mask)[0]
    cells = []
    if indices.size:
        previous = indices[0]
        cell = [previous]
        for  i in indices[1:]:
            if i == previous + 1:
                cell.append(i)
            else:
                cells.append(cell)
                cell = [i]
            previous = i
        cells.append(cell)
    return cells

def _polyfit_masked(x, y, deg, mask):
    cells = _cells(mask)
    l_cells = len(cells)
    ww = np.empty(l_cells)
    pp = np.empty((deg + 1, l_cells))
    for i in range(l_cells):
        ww[i] = len(cells[i])
        if ww[i] <= deg + 1:
            ww[i] = 0
        else:
            pp[:, i] = np.polyfit(x[cells[i]], y[cells[i]], deg)
    w = np.sum(ww)
    p = np.empty(deg + 1)
    if w:
       for i in range(deg + 1):
           p[i] = np.sum(pp[i] * ww) / w
    w = 1. * w / len(x)
    return p, w

def _polyfit2d(x, y, z, order):
    ncols = (order + 1) ** 2
    g = np.zeros((x.size, ncols))
    ij = itertools.product(list(range(order + 1)), list(range(order + 1)))
    for k, (i, j) in enumerate(ij):
        g[:, k] = x ** i * y ** j
    m, _, _, _ = np.linalg.lstsq(g, z, rcond = None)
    return m

def _polyval2d(x, y, m):
    order = int(np.sqrt(len(m))) - 1
    ij = itertools.product(list(range(order + 1)), list(range(order + 1)))
    z = np.zeros_like(x)
    for a, (i, j) in zip(m, ij):
        z += a * x ** i * y ** j
    return z

def xy(im_in, mask):
    spiepy_image_structure = type(im_in) == Im
    if spiepy_image_structure:
        im = np.copy(im_in.data)
    else:
        im = np.copy(im_in)
    im = im.astype('float')
    mask = np.array(mask)
    masked = im.shape == mask.shape and mask.dtype == 'bool'
    lines, points = im.shape
    x_fit = np.arange(points)
    y_fit = np.arange(lines)
    im_plane_x, im_plane_y = np.meshgrid(x_fit.astype('float'),
                                         y_fit.astype('float'))
    m = np.empty(lines)
    w = np.ones(lines)
    for y in y_fit:
        if masked:
            p, w[y] = _polyfit_masked(x_fit, im[y], 1, mask[y])
        else:
            p = np.polyfit(x_fit, im[y], 1)
        m[y] = p[0]
    sum_w = np.sum(w)
    if sum_w:
        im_plane_x *= np.sum(m * w) / sum_w
    else:
        im_plane_x *= 0
    m = np.empty(points)
    w = np.ones(points)
    for x in x_fit:
        if masked:
            p, w[x] = _polyfit_masked(y_fit, im[:, x], 1, mask[:, x])
        else:
            p = np.polyfit(y_fit, im[:, x], 1)
        m[x] = p[0]
    sum_w = np.sum(w)
    if sum_w:
        im_plane_y *= np.sum(m * w) / sum_w
    else:
        im_plane_y *= 0

    im_plane = im_plane_x + im_plane_y
    im -= im_plane
    if masked:
        sum_inv_mask = np.sum(~mask)
        if sum_inv_mask:
            offset = np.sum(im * ~mask) / sum_inv_mask
        else:
            offset = 0
    else:
        offset = np.mean(im)
    im -= offset
    im_plane += offset
    if spiepy_image_structure:
        im_out = im_in.copy()
        im_out.data = im
    else:
        im_out = im
    return im_out, im_plane

def poly_xy(im_in, mask, deg):
    order = np.clip(int(deg), 1, 2)
    if deg != order:
        print('Warning: order of polynomial changed to {:d}.'.format(order))
    spiepy_image_structure = type(im_in) == Im
    if spiepy_image_structure:
        im = np.copy(im_in.data)
    else:
        im = np.copy(im_in)
    im = im.astype('float')
    mask = np.array(mask)
    if not (im.shape == mask.shape and mask.dtype == 'bool'):
        mask = np.zeros_like(im, 'bool')
    yn, xn = im.shape
    xx, yy = np.meshgrid(np.arange(xn, dtype = 'float'),
                         np.arange(yn, dtype = 'float'))
    x = np.ma.array(xx, mask = mask).ravel().compressed()
    y = np.ma.array(yy, mask = mask).ravel().compressed()
    z = np.ma.array(im, mask = mask).ravel().compressed()
    coef = _polyfit2d(x, y, z, order)
    im_plane = _polyval2d(xx, yy, coef)
    im -= im_plane
    if spiepy_image_structure:
        im_out = im_in.copy()
        im_out.data = im
    else:
        im_out = im
    return im_out, im_plane

def by_iterate_mask(im_in, mask_method, fit_type, max_change, max_iterations,
                    mask_options, master_mask):
    spiepy_image_structure = type(im_in) == Im
    if spiepy_image_structure:
        im = np.copy(im_in.data)
        im_out = im_in.copy()
        im_in = np.copy(im_in.data)
    else:
        im = np.copy(im_in)
    if not mask_method in ['peaks', 'mean']:
        mask_method = 'mean'
        print('Warning: mask_method changed to \'mean\'.')
    if not fit_type in ['xy', 'poly1', 'poly2']:
        fit_type = 'xy'
        print('Warning: fit_type changed to \'xy\'.')
    if fit_type == 'poly1':
        order = 1
    elif fit_type == 'poly2':
        order = 2
    master_mask = np.array(master_mask)
    master = im.shape == master_mask.shape and master_mask.dtype == 'bool'
    if mask_method == 'peaks':
        if not (isinstance(mask_options, list) and len(mask_options) == 3 and
                (isinstance(mask_options[0], list) or
                 isinstance(mask_options[0], tuple))):
            mask_options = [(0.3, 0.3), 1, 3.14]
            print('Warning: error in mask_options, using default values: {0}.'.
                  format(mask_options))
    else:
        if not (isinstance(mask_options, list) and len(mask_options) == 3):
            mask_options = [1, 'a', 5]
            print('Warning: error in mask_options, using default values: {0}'.
                  format(mask_options))
    if fit_type == 'xy':
        im, _ = xy(im, master_mask)
    else:
        im, _ = poly_xy(im, master_mask, order)
    if master:
        mask = np.copy(master_mask)
    else:
        mask = np.zeros_like(im, 'bool')
    converged = False
    n = 0
    while not converged and n < max_iterations:
        n += 1
        old_mask = np.copy(mask)
        if mask_method == 'peaks':
            mask, p = _mask.by_troughs_and_peaks(im, master_mask,
                                                 *mask_options[0:2])
            dist, _, _ = locate._average_minimum_distance(p)
            min_size = mask_options[2] * dist ** 2
        else:
            mask = _mask.by_mean(im, master_mask, *mask_options[0:2])
            min_size = mask_options[2]
        if master:
            mask = mask | master_mask
        mask = _mask.tidy_mask(mask, min_size)
        if fit_type == 'xy':
            im, _ = xy(im_in, mask)
        else:
            im, _ = poly_xy(im_in, mask, order)
        change = np.sum(old_mask != mask)
        if change <= max_change:
            converged = True
    if not converged:
        n = 0
        print('Warning: mask did not converge.')
    if spiepy_image_structure:
        im_out.data = im
    else:
        im_out = im
    return im_out, mask, n

def by_peaks(im_in, mask, deg, sigma):
    spiepy_image_structure = type(im_in) == Im
    if spiepy_image_structure:
        im = np.copy(im_in.data)
    else:
        im = np.copy(im_in)
    mask = np.array(mask)
    masked = im.shape == mask.shape and mask.dtype == 'bool'
    p = locate.troughs_and_peaks(im, sigma)
    if masked:
        p = locate.remove_masked_points(p, mask)
    peak_mask = np.zeros_like(im, 'bool')
    peak_mask[p[1]] = True
    im, im_plane = poly_xy(im, ~peak_mask, deg)
    if spiepy_image_structure:
        im_out = im_in.copy()
        im_out.data = im
    else:
        im_out = im
    return im_out, im_plane
