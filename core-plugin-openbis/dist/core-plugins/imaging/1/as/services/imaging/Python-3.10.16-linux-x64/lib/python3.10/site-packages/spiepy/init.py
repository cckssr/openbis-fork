# -*- coding: utf-8 -*-
#
#   Copyright © 2014 Stephan Zevenhuizen
#   SPIEPy, (06-12-2014).
#

import numpy as np
import matplotlib.colors as mcolors
import copy

class Im(object):

    def __init__(self):
        self.data = np.array([[]])
        
    def copy(self):
        return copy.copy(self)

def nanomap():
    begin_color = '#000000'
    mid_color = '#ff8000'
    end_color = '#ffffff'
    c_list = [begin_color, mid_color, end_color]
    return mcolors.LinearSegmentedColormap.from_list('nanomap', c_list)
