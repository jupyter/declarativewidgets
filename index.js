// Copyright (c) Jupyter Development Team.
// Distributed under the terms of the Modified BSD License.

// This is the main file that will be loaded when urth-widgets is required in an
// npm environment.
//
//module.exports = function createDefine(targetModule) {
//    var amdefine = require('amdefine')(targetModule, require);
//
//    return function define() {
//        var args = Array.prototype.slice.call(arguments);
//        //if (args.length > 1) {
//        //    args[0] = args[0].map(function(arg) {
//        //        if (arg === 'jqueryui') arg = 'jquery';
//        //        arg = arg.replace('nbextensions/widgets/widgets/css/', '../css/');
//        //        arg = arg.replace('nbextensions/widgets/components/require-css/css!', '');
//        //        return arg;
//        //    });
//        //}
//        amdefine.apply(this, args);
//    };
//};

global.jQuery = global.$ = require('jquery');
var init = require('./dist/urth_widgets/js/init/init');

exports.init =  init;

var Events = function () {};

var events = new Events();


//shim in IPython
IPython = {
    notebook: {
        events: $([events])
    }
}
