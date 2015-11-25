# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

import os
from setuptools import setup, find_packages
from setuptools.command.install import install

from notebook.nbextensions import install_nbextension
from notebook.services.config import ConfigManager
from jupyter_core.paths import jupyter_config_dir


# Get location of this file at runtime
HERE = os.path.abspath(os.path.dirname(__file__))

# Eval the version tuple and string from the source
VERSION_NS = {}
with open(os.path.join(HERE, 'urth/widgets/ext/_version.py')) as f:
    exec(f.read(), {}, VERSION_NS)

EXT_DIR = os.path.join(HERE, 'urth_widgets')
SERVER_EXT_CONFIG = "c.NotebookApp.server_extensions.append('urth.widgets.ext.urth_import')"
VERSION_FILE = os.path.join(HERE, 'VERSION')

class InstallCommand(install):
    def run(self):
        print('Installing Python module')
        install.run(self)

        print('Installing notebook extension')
        install_nbextension(EXT_DIR, overwrite=True, user=True)
        cm = ConfigManager()
        print('Enabling extension for notebook')
        cm.update('notebook', {"load_extensions": {'urth_widgets/js/main': True}})

        print('Installing notebook server extension')
        fn = os.path.join(jupyter_config_dir(), 'jupyter_notebook_config.py')

        if os.path.isfile(fn):
            with open(fn, 'r+') as fh:
                lines = fh.read()
                if SERVER_EXT_CONFIG not in lines:
                    fh.seek(0, 2)
                    fh.write('\n')
                    fh.write(SERVER_EXT_CONFIG)
        else:
            with open(fn, 'w') as fh:
                fh.write('c = get_config()\n')
                fh.write(SERVER_EXT_CONFIG)

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

setup(
    name='jupyter_declarativewidgets',
    author='Jupyter Development Team',
    author_email='jupyter@googlegroups.com',
    description='IPython / Jupyter extensions for supporting declarative widgets',
    url='https://github.com/jupyter-incubator/declarativewidgets',
    version=VERSION_NS['__version__'],
    license='BDS',
    platforms=['IPython Notebook 3.x'],
    packages=find_packages(),
    include_package_data=True,
    install_requires=[],
    cmdclass={
        'install': InstallCommand
    }
)
