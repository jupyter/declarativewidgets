# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

import os
import sys
from setuptools import setup, find_packages

# Get location of this file at runtime
HERE = os.path.abspath(os.path.dirname(__file__))

# Eval the version tuple and string from the source
VERSION_NS = {}
with open(os.path.join(HERE, 'urth/widgets/ext/_version.py')) as f:
    exec(f.read(), {}, VERSION_NS)

VERSION_FILE = os.path.join(HERE, 'VERSION')

# Apply version to build
if os.path.isfile(VERSION_FILE):
    # CI build, read metadata and append
    with open(VERSION_FILE, 'r') as fh:
        BUILD_INFO = fh.readline().strip()
        fh.close()

    with open(VERSION_FILE, 'w') as fh:
        fh.write(VERSION_NS['__version__'] + '\n')
        fh.write(BUILD_INFO + '\n')
        fh.close()

setup_args = dict(
    name='jupyter_declarativewidgets',
    author='Jupyter Development Team',
    author_email='jupyter@googlegroups.com',
    description='Jupyter extensions for supporting declarative widgets',
    url='https://github.com/jupyter-incubator/declarativewidgets',
    version=VERSION_NS['__version__'],
    license='BDS',
    platforms=['Jupyter Notebook 4.0.x'],
    packages=find_packages(),
    include_package_data=True,
    scripts=[
        'scripts/jupyter-declarativewidgets'
    ]
)

if 'setuptools' in sys.modules:
    # setupstools turns entrypoint scripts into executables on windows
    setup_args['entry_points'] = {
        'console_scripts': [
            'jupyter-declarativewidgets = declarativewidgets.extensionapp:main'
        ]
    }
    # Don't bother installing the .py scripts if if we're using entrypoints
    setup_args.pop('scripts', None)

if __name__ == '__main__':
    setup(**setup_args)


