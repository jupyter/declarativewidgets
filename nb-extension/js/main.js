/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
define([
    'base/js/namespace',
    'base/js/events',
    'nbextensions/widgets/widgets/js/init',
    'nbextensions/widgets/widgets/js/widget',
    './init/init'
], function(Jupyter, events, widgetManager, ipywidget, init) {
    'use strict';

    // Some versions of IE do not have window.console defined. Some versions
    // do not define the debug and other methods. This is a minimal workaround
    // based on what declarative widgets code is using.
    window.console = window.console || {};
    window.console.log = window.console.log || function() {};
    ['debug', 'error', 'trace', 'warn'].forEach(function(method) {
        window.console[method] = window.console[method] || window.console.log;
    });
    
    init({
        namespace: Jupyter,
        events: events,
        WidgetManager: widgetManager.WidgetManager,
        WidgetModel: ipywidget.WidgetModel
    });

    return {
        load_ipython_extension: function() { console.debug('Custom JS loaded'); }
    };
});
