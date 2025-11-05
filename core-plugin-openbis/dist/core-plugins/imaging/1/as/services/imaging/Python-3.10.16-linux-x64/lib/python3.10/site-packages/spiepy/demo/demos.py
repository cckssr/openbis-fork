# -*- coding: utf-8 -*-
#
#   Copyright © 2014 Stephan Zevenhuizen
#   SPIEPy, (30-11-2014).
#

import numpy as np
import os
from struct import unpack

def list_demo_files():
    here = os.path.abspath(os.path.dirname(__file__))
    l = [i for i in os.listdir(here) if i[-4:] == '.dat']
    l.sort()
    demos = dict(enumerate(l))
    return demos

def load_demo_file(f_name):
    here = os.path.abspath(os.path.dirname(__file__))
    f_path = os.path.join(here, f_name)
    with open(f_path, 'rb') as f:
        shape = unpack('<LL', f.read(8))
        fmt = '<%dd' % (shape[0] * shape[1])
        im = np.array(unpack(fmt, f.read())).reshape(shape)
    return im
