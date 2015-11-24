/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
define([
    './init/init'
], function(init) {
    'use strict';
    init(IPython ? IPython.notebook.base_url : '/');
    return {
      load_ipython_extension: function() { console.debug('Custom JS loaded'); }
    };
});
