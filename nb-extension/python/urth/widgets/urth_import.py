# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.

import codecs
import os
import json
import subprocess
import tornado.web

from IPython.html.utils import url_path_join
from IPython.utils.path import get_ipython_dir
from IPython.html.base.handlers import FileFindHandler
from tornado.gen import coroutine, Return, Task
from tornado.process import Subprocess
from tornado.web import HTTPError, RequestHandler
from contextlib import redirect_stdout

@coroutine
def call_subprocess(cmd):
    '''
    Spawns a subprocess and streams its stdout and stderr asynchronously.
    Sets the optional environment variables and feeds optional data on stdin.
    :param cmd:
    :param env:
    :param stdin_data:
    '''
    sub_process = Subprocess(
        cmd,
        stdin=subprocess.PIPE,
        stdout=Subprocess.STREAM,
        stderr=Subprocess.STREAM
    )

    result, error = yield [
        Task(sub_process.stdout.read_until_close),
        Task(sub_process.stderr.read_until_close)
    ]

    raise Return((result, error))

class UrthImportHandler(RequestHandler):

    ipython_dir = get_ipython_dir()
    nbext = os.path.join(ipython_dir, 'nbextensions/urth_widgets/')

    # This API can be used to retrieve the list of installed bower packages.
    # It is not currently being used by the client.
    def get(self):
        os.chdir(self.nbext)
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

    @coroutine
    def post(self):
        if '.bowerrc' not in os.listdir():
            print("writing .bowerrc in", os.getcwd())
            with open('.bowerrc', 'a') as f:
                f.write("""{
                "analytics": false,
                "interactive": false
                }""")

        package_name = json.loads(self.request.body.decode())['package']
        os.chdir(self.nbext)
        install_cmd = ['bower', 'install', '--allow-root', '--config.interactive=false', package_name]

        try:
            install_result, install_error = yield call_subprocess(install_cmd)
        except subprocess.CalledProcessError as e:
            msg = 'Failed to install {0}.'.format(package_name)
            raise tornado.web.HTTPError(400, msg, reason=msg)

        info_cmd = ['bower', 'info', '--allow-root', '-o',
            '--config.interactive=false', package_name, 'name']

        try:
            info_result, info_error = yield call_subprocess(info_cmd)
        except Exception as e:
            msg = 'Failed to get info about {0}.'.format(package_name)
            raise tornado.web.HTTPError(500, msg, reason=msg)

        directory_name = str(info_result).split("'")[1]
        path = os.path.join('bower_components', directory_name, 'bower.json')

        try:
            with open(path) as f:
                content = f.read()
                self.set_status(200)
                self.finish(json.dumps(content))
        except Exception as e:
            msg = 'Failed to open bower.json for {0}.'.format(package_name)
            raise tornado.web.HTTPError(404, msg, reason=msg)

class ComponentsRedirectHandler(RequestHandler):
    def get(self, path):
        '''
        Redirect relative requests for components to the global store. Makes
        components easier to relocate later.
        '''
        url = url_path_join(self.settings['base_url'], 'urth_components', path)
        self.redirect(url, permanent=True)

def load_jupyter_server_extension(nb_app):
    '''
    Register a Urth import handler.
    '''
    web_app = nb_app.web_app
    host_pattern = '.*$'
    ipython_dir = get_ipython_dir()
    for path in web_app.settings['nbextensions_path']:
        if ipython_dir in path:
            nbext = path
    import_route_pattern = url_path_join(web_app.settings['base_url'], '/urth_import')

    # Any requests containing /urth_components/ will get served from the bower_components
    # directory.
    components_route_pattern = url_path_join(web_app.settings['base_url'], '/urth_components/(.*)')
    path = os.path.join(nbext, 'urth_widgets/bower_components/')

    web_app.add_handlers(host_pattern, [
        (import_route_pattern, UrthImportHandler),
        (components_route_pattern, FileFindHandler, {'path': [path]})
    ])
