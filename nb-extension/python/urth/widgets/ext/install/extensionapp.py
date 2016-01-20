# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.
import errno
import os.path
import sys

from jupyter_core.paths import jupyter_config_dir
from notebook.services.config import ConfigManager
from notebook.nbextensions import (InstallNBExtensionApp, EnableNBExtensionApp, 
    DisableNBExtensionApp, flags, aliases)
from traitlets import Unicode
from traitlets.config.application import catch_config_error
from traitlets.config.application import Application

# Make copies to reuse flags and aliases
INSTALL_FLAGS = {}
INSTALL_FLAGS.update(flags)

INSTALL_ALIASES = {}
INSTALL_ALIASES.update(aliases)
del INSTALL_ALIASES['destination']

def makedirs(path):
    '''
    mkdir -p and ignore existence errors compatible with Py2/3.
    '''
    try:
        os.makedirs(path)
    except OSError as e:
        if e.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise

class ExtensionInstallApp(InstallNBExtensionApp):
    '''Subclass that installs this particular extension.'''
    name = u'jupyter-declarativewidgets-extension-install'
    description = u'Install the jupyter_declarativewidgets extension'

    flags = INSTALL_FLAGS
    aliases = INSTALL_ALIASES

    examples = """
        jupyter declarativewidgets install
        jupyter declarativewidgets install --user
        jupyter declarativewidgets install --prefix=/path/to/prefix
        jupyter declarativewidgets install --nbextensions=/path/to/nbextensions
    """

    destination = Unicode('')

    def _classes_default(self):
        return [ExtensionInstallApp, InstallNBExtensionApp]

    def start(self):
        here = os.path.abspath(os.path.join(os.path.dirname(__file__)))

        self.log.info("Installing jupyter_declarativewidgets notebook extensions")
        self.extra_args = [os.path.join(here, '../notebook')]
        self.destination = 'urth_widgets'  #TODO: must change to jupyter_declarativewidgets
        self.install_extensions()


class ExtensionActivateApp(EnableNBExtensionApp):
    '''Subclass that activates this particular extension.'''
    name = u'jupyter-declarativewidgets-extension-activate'
    description = u'Activate the jupyter_declarativewidgets extension'

    flags = {}
    aliases = {}

    examples = """
        jupyter declarativewidgets activate
    """

    def _classes_default(self):
        return [ExtensionActivateApp, EnableNBExtensionApp]

    def enable_server_extension(self, extension):
        '''Enables the server side extension in the user config.'''
        server_cm = ConfigManager(config_dir=jupyter_config_dir())
        
        makedirs(server_cm.config_dir)

        cfg = server_cm.get('jupyter_notebook_config')
        server_extensions = (
            cfg.setdefault('NotebookApp', {})
            .setdefault('server_extensions', [])
        )
        if extension not in server_extensions:
            cfg['NotebookApp']['server_extensions'] += [extension]
        server_cm.update('jupyter_notebook_config', cfg)

    def start(self):
        self.log.info("Activating jupyter_declarativewidgets notebook server extensions")
        self.enable_server_extension('urth.widgets.ext.urth_import')

        self.log.info("Activating jupyter_declarativewidgets JS notebook extensions")
        self.section = "notebook"
        self.enable_nbextension("urth_widgets/js/main")

        self.log.info("Done. You may need to restart the Jupyter notebook server for changes to take effect.")

class ExtensionDeactivateApp(DisableNBExtensionApp):
    '''Subclass that deactivates this particular extension.'''
    name = u'jupyter-declarativewidgets-extension-deactivate'
    description = u'Deactivate the jupyter_declarativewidgets extension'

    flags = {}
    aliases = {}

    examples = """
        jupyter declarativewidgets deactivate
    """

    def _classes_default(self):
        return [ExtensionDeactivateApp, DisableNBExtensionApp]

    def disable_server_extension(self, extension):
        '''Disables the server side extension in the user config.'''
        server_cm = ConfigManager(config_dir=jupyter_config_dir())
        
        makedirs(server_cm.config_dir)

        cfg = server_cm.get('jupyter_notebook_config')
        if ('NotebookApp' in cfg and
            'server_extensions' in cfg['NotebookApp'] and
            extension in cfg['NotebookApp']['server_extensions']):
            cfg['NotebookApp']['server_extensions'].remove(extension)

        server_cm.update('jupyter_notebook_config', cfg)

        server_extensions = (
            cfg.setdefault('NotebookApp', {})
            .setdefault('server_extensions', [])
        )
        if extension in server_extensions:
            cfg['NotebookApp']['server_extensions'].remove(extension)
        server_cm.update('jupyter_notebook_config', cfg)

    def start(self):
        self.log.info("Deactivating jupyter_declarativewidgets notebook server extensions")
        self.disable_server_extension('urth.widgets.ext.urth_import')

        self.log.info("Deactivating jupyter_declarativewidgets JS notebook extensions")
        self.section = "notebook"
        self.disable_nbextension("urth_widgets/js/main")

        self.log.info("Done. You may need to restart the Jupyter notebook server for changes to take effect.")

class ExtensionApp(Application):
    '''CLI for extension management.'''
    name = u'jupyter_declarativewidgets extension'
    description = u'Utilities for managing the jupyter_declarativewidgets extension'
    examples = ""

    subcommands = dict(
        install=(
            ExtensionInstallApp,
            "Install the extension."
        ),
        activate=(
            ExtensionActivateApp,
            "Activate the extension."
        ),
        deactivate=(
            ExtensionDeactivateApp,
            "Deactivate the extension."
        )
    )

    def _classes_default(self):
        classes = super(ExtensionApp, self)._classes_default()

        # include all the apps that have configurable options
        for appname, (app, help) in self.subcommands.items():
            if len(app.class_traits(config=True)) > 0:
                classes.append(app)

    @catch_config_error
    def initialize(self, argv=None):
        super(ExtensionApp, self).initialize(argv)

    def start(self):
        # check: is there a subapp given?
        if self.subapp is None:
            self.print_help()
            sys.exit(1)

        # This starts subapps
        super(ExtensionApp, self).start()

def main():
    ExtensionApp.launch_instance()