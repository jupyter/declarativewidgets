# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

def _jupyter_nbextension_paths():
    '''API for JS extension installation on notebook 4.2'''
    return [{
        'section': 'notebook',
        'src': 'static',
        'dest': 'declarativewidgets',
        'require': 'declarativewidgets/js/main'
    }]

def _jupyter_server_extension_paths():
    '''API for server extension installation on notebook 4.2'''
    return [{
        "module": "urth.widgets.ext.urth_import"
    }]