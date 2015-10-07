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
from tornado.web import HTTPError, RequestHandler
from contextlib import redirect_stdout

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

        try:
            subprocess.check_call(['bower', 'install', '--allow-root', '--config.interactive=false', package_name])
        except subprocess.CalledProcessError as e:
            msg = 'Failed to install {0}.'.format(package_name)
            raise tornado.web.HTTPError(400, msg, reason=msg)

        try:
            proc = subprocess.Popen(['bower', 'info', '--allow-root', '-o'
                '--config.interactive=false', package_name, 'name'], stdout=subprocess.PIPE)
        except Exception as e:
            msg = 'Failed to get info about {0}.'.format(package_name)
            raise tornado.web.HTTPError(500, msg, reason=msg)

        output = proc.communicate()
        output_str = str(output[0])
        directory_name = output_str.split("'")[1]
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
