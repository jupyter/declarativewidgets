/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
define([
    './init/init'
], function(init) {
    'use strict';
    init(IPython ? IPython.notebook.base_url : '/');
    console.debug('Custom JS loaded');
});
