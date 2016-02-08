/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
define([
    './init/init'
], function(init) {
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
    return {
      load_ipython_extension: function() { console.debug('Custom JS loaded'); }
    };
});
