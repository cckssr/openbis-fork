# -*- coding: utf-8 -*-
#
#   Copyright © 2014 - 2023 Stephan Zevenhuizen
#   SPIEPy, (24-11-2023).
#

'''
    SPIEPy (Scanning Probe Image Enchanter using Python) is a Python library
    to improve automatic processing of SPM images.
'''


import sys, os
from importlib.metadata import distribution

if os.path.split(__path__[0])[0] in sys.path[1:]:
    __version__ = distribution(__name__).version
else:
    __version__ = '0.0.0'

__all__ = ['NANOMAP', 'Im',
           'flatten_by_iterate_mask', 'flatten_by_peaks', 'flatten_poly_xy',
           'flatten_xy',
           'locate_masked_points_and_remove', 'locate_troughs_and_peaks',
           'locate_steps', 'locate_regions',
           'mask_by_mean', 'mask_by_troughs_and_peaks', 'mask_tidy',
           'measure_feature_properties']

from . import init

NANOMAP = init.nanomap()


class Im(init.Im):
    '''
    SPIEPy_image_structure

    The attribute data is mandatory and contains the 2D array of image data.
    '''


from . import flatten, locate, measure
from . import mask as _mask

def flatten_by_iterate_mask(im_in, mask_method = 'mean', fit_type = 'poly1',
                            max_change = 10, max_iterations = 100,
                            mask_options = [1, 'a', 5], master_mask = None):
    '''
    Iteratively improves flattening using masking.

    Repeats the masking and flattening in a loop until the change in the mask
    is withing an acceptable number of pixels.

    Args:
        im_in : SPIEPy_image_structure or ndarray
            Contains the 2D array of image data.

    Kwargs:
        mask_method : str
            'peaks' for masking method mask_by_troughs_and_peaks, 'mean' for
            masking method mask_by_mean.
        fit_type : str
            'xy' for fit method flatten_xy, 'poly1' and 'poly2' for fit method
            flatten_poly_xy with respectively order 1 and 2.
        max_change : int
            Convergence conditions. Specifies the maximum number of pixels
            allowed to change in a mask between iterations. If the number of
            pixels is above this the mask is not considered converged, and
            another iteration is performed.
        max_iterations : int
            Specifies the maximum number of iterations to be performed.
        mask_options : list
            List of 3 items which dependents on the masking method.
            Item 1 is mult (mask_by_troughs_and_peaks) or mult (mask_by_mean).
            Item 2 is sigma (mask_by_troughs_and_peaks) or mode (mask_by_mean).
            Item 3 is a multiplier in minimum size (mask_by_troughs_and_peaks)
            or the minimum size (mask_by_mean) in the mask_tidy method.
        master_mask : ndarray or None
            Should be a 2D logical array the shape of the image data. Pixels in
            the image which match TRUE pixels in the mask, are considered
            "masked out" and will always be in the the mask used for flattening.

    Returns:
        im_out : SPIEPy_image_structure or ndarray
            Final flattened image.
        mask : ndarray
            The mask used to generate the final flattened image.
        n : int
            The number of iterations used to generate mask. If mask did not
            converge n is set to zero.
    '''
    return flatten.by_iterate_mask(im_in, mask_method, fit_type, max_change,
                                   max_iterations, mask_options, master_mask)

def flatten_by_peaks(im_in, mask = None, deg = 2, sigma = 1):
    '''
    Polynomial plane flattening only on peaks in image.

    Args:
        im_in : SPIEPy_image_structure or ndarray
            Contains the 2D array of image data.

    Kwargs:
        mask : ndarray or None
            Should be a 2D logical array the shape of the image data. Pixels in
            the image which match TRUE pixels in the mask, are considered
            "masked out" and will not be used for flattening.
        deg : int
            Order of the polynomial, should be 1 or 2.
        sigma : float
            Standard deviation for Gaussian kernel. For noise filtering of the
            image.

    Returns:
        im_out : SPIEPy_image_structure or ndarray
            Is input image minus fitted plane.
        im_plane : ndarray
            Is the plane which has been subtracted from the image.
    '''
    return flatten.by_peaks(im_in, mask, deg, sigma)

def flatten_poly_xy(im_in, mask = None, deg = 2):
    '''
    Performs a polynomial plane fit of order 'deg' using the method of
    least squares.

    Args:
        im_in : SPIEPy_image_structure or ndarray
            Image to be flattened.

    Kwargs:
        mask : ndarray or None
            Should be a 2D logical array the shape of the image data. Pixels in
            the image which match TRUE pixels in the mask, are considered
            "masked out" and will not be used for flattening.
        deg : int
            Order of the polynomial, should be 1 or 2.

    Returns:
        im_out : SPIEPy_image_structure or ndarray
            Is input image minus fitted plane.
        im_plane : ndarray
            Is the plane which has been subtracted from the image.
    '''
    return flatten.poly_xy(im_in, mask, deg)

def flatten_xy(im_in, mask = None):
    '''
    Flattens image with first order polynomial plane.

    Performs a first order polynomial plane fit using an algorithm of
    average line fits. This method is less susceptible to localised image
    anomalies, than true plane fitting, but is only applicable for first
    order planes.

    Args:
        im_in : SPIEPy_image_structure or ndarray
            Image to be flattened.

    Kwargs:
        mask : ndarray or None
            Should be a 2D logical array the shape of the image data. Pixels in
            the image which match TRUE pixels in the mask, are considered
            "masked out" and will not be used for flattening.

    Returns:
        im_out : SPIEPy_image_structure or ndarray
            Is input image minus fitted plane.
        im_plane : ndarray
            Is the plane which has been subtracted from the image.
    '''
    return flatten.xy(im_in, mask)

def locate_masked_points_and_remove(p, mask):
    '''
    Removes points from a list if they are found in a mask.

    Args:
        p : list
            Vectors for troughs and peaks of all of the x and y coordinates.
        mask : ndarray
            Should be a 2D logical array the shape of the image data. Any listed
            points in a TRUE area of the mask are removed from the list.

    Returns:
        p : list
            Vectors for troughs and peaks of all of the x and y coordinates that
            are not masked.
    '''
    return locate.remove_masked_points(p, mask)

def locate_troughs_and_peaks(im, sigma = 1):
    '''
    Locates troughs and peaks in the image.

    Similar to the Python module (scipy.ndimage.filters) functions
    maximum_filter and minimum_filter except the image is first filtered
    to remove noise. Also anything located touching the edge of the image
    is not returned as it is probably not a true maxima/minima of the
    surface.

    Args:
        im : SPIEPy_image_structure or ndarray
            Contains the 2D array of image data.

    Kwargs:
        sigma : float
            Standard deviation for Gaussian kernel. For noise filtering of the
            image.

    Returns:
        p : list
            Vectors for troughs and peaks of all of the x and y coordinates.
    '''
    return locate.troughs_and_peaks(im, sigma)

def locate_steps(im, sigma = 2, low_threshold = 0.1, high_threshold = 0.2):
    '''
    Locates step edges in an image.

    Uses the Canny edge finding routine, DOI: 10.1109/TPAMI.1986.4767851,
    to locate step edges in an image. The image should be flattened before
    input for best results. For images with atomic resolution consider using
    locate_regions to create the input image.

    Args:
        im : SPIEPy_image_structure or ndarray
            Contains the 2D array of image data.

    Kwargs:
        sigma : float
            Standard deviation for Gaussian kernel. Very important parameter.
        low_threshold : float
            The low threshold for the thresholding with hysteresis method.
        high_threshold : float
            The high threshold for the thresholding with hysteresis method.

    Returns:
        mask : ndarray
            The output is a 2D logical array with the shape of the image data.
    '''
    return locate.find_steps(im, sigma, low_threshold, high_threshold)

def locate_regions(im_in, sigma = 1, mask = None):
    '''
    Increases terrace contrast on atomic resolution images.

    An image is produced where the contrast of the atoms is being reduced
    so that the terrace contrast becomes more clearer.

    Args:
        im_in : SPIEPy_image_structure or ndarray
            Contains the 2D array of image data.

    Kwargs:
        sigma : float
            Standard deviation for Gaussian kernel. For noise filtering of the
            image.
        mask : ndarray
            Should be a 2D logical array the shape of the image data. All
            contrast will be removed from the masked areas.

    Returns:
        im_out : SPIEPy_image_structure or ndarray
            Is input image with adjusted contrast.
        peaks : tuple
            Vectors for peaks (atoms) of all of the x and y coordinates that are
            not masked.
    '''
    return locate.regions(im_in, sigma, mask)

def mask_by_mean(im, master_mask = None, mult = 1, mode = 'a'):
    '''
    Generates a mask of high and low areas defined by the mean.

    Args:
        im : SPIEPy_image_structure or ndarray
            Contains the 2D array of image data.

    Kwargs:
        master_mask : ndarray or None
            Logical array with the shape of image data. Any TRUE areas in the
            master_mask are not used by any parts of the function. Allows the
            function to effectively be run on a section of an image.
        mult : float
            The number of standard deviations from the average is masked.
        mode : str
            3 possible modes selected by strings. "a" is all high and low areas
            are masked, "l" only low areas are masked, "h" only high areas are
            masked.

    Returns:
        mask : ndarray
            The output is a 2D logical array with the shape of the image data.
    '''
    return _mask.by_mean(im, master_mask, mult, mode)

def mask_by_troughs_and_peaks(im, master_mask = None, mult = (0.3, 0.3),
                              sigma = 1):
    '''
    Generates a mask of high and low areas of an image.

    The function uses troughs and peaks in the image to find the mean trough and
    peak height. The difference between these gives the surfaces corrugation.
    Any pixel more than 0.3 the surface corrugation above the mean peak height
    or 0.3 the surface corrugation below the mean trough height is masked TRUE.

    Args:
        im : SPIEPy_image_structure or ndarray
            Contains the 2D array of image data.

    Kwargs:
        master_mask : ndarray or None
            Logical array with the shape of image data. Any TRUE areas in the
            master_mask are not used by any parts of the function. Allows the
            function to effectively be run on a section of an image.
        mult : tuple of two floats
            Modifies the multiple of the corrugation height used. For example if
            mult[1] = 0.7, high areas are now defined as 0.7 times the
            corrugation height above the mean peak height. Same for low areas
            using mult[0].
        sigma : float
            The filter parameter used to locate the troughs and peaks, explained
            in detail in the help for locate_troughs_and_peaks.

    Returns:
        mask : ndarray
            The output is a 2D logical array with the shape of the image data.
        p : list
            Vectors for troughs and peaks of all of the x and y coordinates that
            are not masked.
    '''
    return _mask.by_troughs_and_peaks(im, master_mask, mult, sigma)

def mask_tidy(mask, min_size = 5):
    '''
    Cleans up masks.

    Holes in masked areas and small masked areas are removed.

    Args:
        mask : ndarray
            Should be a 2D logical array the shape of the image data.

    Kwargs:
        min_size: int
            Minimal area size of object in mask.

    Returns:
        mask : ndarray
            The output is a 2D logical array with the shape of the image data.
    '''
    return _mask.tidy_mask(mask, min_size)

def measure_feature_properties(im, p, thresh_frac = 0.8, box_mult = 10.0):
    '''
    Analyses surface features such as atoms or molecules.

    Feature and minimum coordinates can be generated with
    locate_troughs_and_peaks. Features are masked and then analysed.

    Args:
        im : SPIEPy_image_structure or ndarray or MaskedArray
            Contains the 2D array of image data.
        p : list
            Vectors for troughs and peaks.

    Kwargs:
        thresh_frac : float
            Specifies the masking threshold as a fraction of the height
            difference between the feature and its nearest local minimum.
        box_mult : float
            Features are analysed in a box centred on the feature, of width
            box_mult * 2 * d, where d is the distance from the feature to its
            nearest local minimum. Set higher for features known to be highly
            non-circular. Any feature which has a mask which touches the edge
            of its analysis box is deemed to be poorly resolved and is not
            included in the statistics.

    Returns:
        properties : dictionary
            eccentricity : float
                The average eccentricity (0 is circular, < 1 is elliptic).
            area : float
                The average area.
            count_good : int
                The number of features used for statistics.
            mask_good : ndarray
                The output is a 2D logical array with the shape of the image
                data. Masks all features used for statistics.
            mask_all : ndarray
                The output is a 2D logical array with the shape of the image
                data. Masks all features.
    '''
    return measure.feature_properties(im, p, thresh_frac, box_mult)
