/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
define([
    'module',
    './init/init'
], function(module, init) {
    'use strict';

    // Some versions of IE do not have window.console defined. Some versions
    // do not define the debug and other methods. This is a minimal workaround
    // based on what declarative widgets code is using.
    window.console = window.console || {};
    window.console.log = window.console.log || function() {};
    ['debug', 'error', 'trace', 'warn'].forEach(function(method) {
        window.console[method] = window.console[method] || window.console.log;
    });
    
    init(IPython ? IPython.notebook.base_url : '/');

    var getModuleBasedComponentRoot = function() {
        var moduleuri = module.uri;
        return moduleuri.substring(0, moduleuri.lastIndexOf('/'));
    }

    var load_css = function (name) {
        var link = document.createElement("link");
        link.type = "text/css";
        link.rel = "stylesheet";
        link.href = name;
        document.getElementsByTagName("head")[0].appendChild(link);
      };

    load_css(getModuleBasedComponentRoot() + '/../css/main.css');

    return {
      load_ipython_extension: function() { console.debug('Custom JS loaded'); }
    };
});
