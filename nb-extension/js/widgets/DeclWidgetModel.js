// Copyright (c) Jupyter Development Team.
// Distributed under the terms of the Modified BSD License.

// npm compatibility
function requireLocalFiles() {
    require('../../../../../../node_modules/jupyter-js-widgets/static/widgets/js/manager-base');
    require('../../../../../../node_modules/jupyter-js-widgets/static/widgets/js/widget');
}

if (typeof define !== 'function') {
    var amdefine = require('amdefine')(module, require);

    var mapping = {}
    mapping['nbextensions/widgets/widgets/js/manager'] = '../../../../../../node_modules/jupyter-js-widgets/static/widgets/js/manager-base'
    mapping['nbextensions/widgets/widgets/js/widget'] = '../../../../../../node_modules/jupyter-js-widgets/static/widgets/js/widget'

    var define = function(){
        var args = Array.prototype.slice.call(arguments);
        if (args.length > 1) {
            args[0] = args[0].map(function(arg) {
                arg = mapping[arg] || arg;
                return arg;
            });
        }
        amdefine.apply(this, args);
    }
}

define(["nbextensions/widgets/widgets/js/manager",
    "nbextensions/widgets/widgets/js/widget"
], function(widgetmanager, ipywidget) {
    "use strict";

    /**
     * Collection of patches on top of ipywidgets.WidgetModel
     */

    var DeclWidgetModel = function(widget_manager, model_id, comm) {
        ipywidget.WidgetModel.apply(this, arguments);
    };

    DeclWidgetModel.prototype = Object.create(ipywidget.WidgetModel.prototype);

    /*
     * Making request_state a noop to avoid requiring this handshake to take place to create model.
     */
    DeclWidgetModel.prototype.request_state = function(callbacks) {
        console.trace( "Empty implementation of request_state()")
        return Promise.resolve(this);
    };

    /*
     * Avoiding out of sequence messages due to new promise/async code in ipywidgets.WidgetModel
     */
    DeclWidgetModel.prototype.send_sync_message = function(attrs, callbacks) {
        var data = {method: 'backbone', sync_data: attrs};
        this.comm.send(data, callbacks);
    };

    widgetmanager.ManagerBase.register_widget_model('DeclWidgetModel', DeclWidgetModel);

    return DeclWidgetModel;

});