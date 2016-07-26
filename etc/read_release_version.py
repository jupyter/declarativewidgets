#!/usr/bin/env python
# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

import os

# Get location of this file at runtime
HERE = os.path.abspath(os.path.dirname(__file__))

# Eval the version tuple and string from the source
VERSION_NS = {}
with open(os.path.join(HERE, 'urth/widgets/ext/_version.py')) as f:
    exec(f.read(), {}, VERSION_NS)
    print(VERSION_NS['__version__'])