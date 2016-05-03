# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

import os
import json
import tornado
import subprocess

from notebook.utils import url_path_join
from notebook.base.handlers import FileFindHandler
from jupyter_core.paths import jupyter_path
from tornado.web import HTTPError, RequestHandler
from concurrent.futures import ThreadPoolExecutor

widgets_dir = ''
logger = None


class UrthImportHandler(RequestHandler):

    def initialize(self, executor):
        self.executor = executor

    # This API can be used to retrieve the list of installed bower packages.
    # It is not currently being used by the client.
    def get(self):
        os.chdir(widgets_dir)
        try:
            proc = subprocess.check_output(['bower', 'list', '--allow-root', '--config.interactive=false', '-o', '-p', '-j'])
            d = json.loads(proc.decode('utf-8'))
            for k in d.keys():
                v = d[k]
                if isinstance(v, list):
                    temp = []
                    for el in v:
                        temp.append(el.replace('bower_', '/'))
                    d[k] = temp
                else:
                    d[k] = v.replace('bower_', '/')
            self.finish(d)

        except subprocess.CalledProcessError as e:
            msg = 'Failed to list bower packages'
            raise tornado.web.HTTPError(400, msg, reason=msg)

    @tornado.gen.coroutine
    def post(self):
        package_name = json.loads(self.request.body.decode())['package']

        # Use the ThreadPoolExecutor to perform the bower install. Since
        # the ThreadPoolExecutor has a max_workers of 1, this allows the
        # installs to happen asynchronously but in the requested order
        # to prevent simultaneous install issues.
        code = yield self.executor.submit(do_install, package_name=package_name)
        if code == 0:
            self.set_status(200)
            self.finish()
        else:
            msg = 'Failed to install {0}.'.format(package_name)
            self.send_error(status_code=400, reason=msg)


# Executes bower install requests in a subprocess and returns 0 for success
# and non-zero for an error.
def do_install(package_name):
    logger.info('Installing {0}'.format(package_name))
    try:
        subprocess.check_call(['bower', 'install', package_name, '--allow-root',
                '--config.interactive=false', '--production', '--quiet'],
                cwd=widgets_dir)
    except subprocess.CalledProcessError as e:
        return -1

    return 0


def load_jupyter_server_extension(nb_app):
    global logger
    global widgets_dir

    logger = nb_app.log
    logger.info('Loading urth_import server extension.')

    web_app = nb_app.web_app
    widgets_dir = get_nbextension_path()

    # Write out a .bowerrc file to configure bower installs to
    # not be interactive and not to prompt for analytics
    bowerrc = os.path.join(widgets_dir, '.bowerrc')
    if os.access(bowerrc, os.F_OK) is not True:
        logger.debug('Writing .bowerrc at {0}'.format(bowerrc))
        with open(bowerrc, 'a') as f:
            f.write("""{
            "analytics": false,
            "interactive": false,
            "directory": "urth_components"
            }""")

    # The import handler serves from /urth_import and any requests
    # containing /urth_components/ will get served from the actual
    # urth_components directory.
    import_route_pattern = url_path_join(web_app.settings['base_url'], '/urth_import')
    components_route_pattern = url_path_join(web_app.settings['base_url'], '/urth_components/(.*)')
    components_path = os.path.join(widgets_dir, 'urth_components/')

    # Register the Urth import handler and static file handler.
    logger.debug('Adding handlers for {0} and {1}'.format(import_route_pattern, components_route_pattern))
    web_app.add_handlers('.*$', [
        (import_route_pattern, UrthImportHandler, dict(executor=ThreadPoolExecutor(max_workers=1))),
        (components_route_pattern, FileFindHandler, {'path': [components_path]})
    ])

def get_nbextension_path():
    """Find the path to the declarativewidgets nbextension"""
    for abspath in jupyter_path('nbextensions', 'declarativewidgets'):
        if os.path.exists(abspath):
            return abspath
