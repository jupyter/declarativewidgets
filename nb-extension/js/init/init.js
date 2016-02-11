/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
/* globals console:false */
/**
 * Loads the web component polyfill and all web components specified in
 * elements.html.
 */
define(['require', 'module', 'jquery'], function(require, module, $) {
    'use strict';

    function loadComponents(bower_root, links) {
        console.debug('Bower root is: ', bower_root);

        var defaultLinks = [
            { href: bower_root + '/urth-core-bind/urth-core-bind.html' },
            (function() {
                var deferred = $.Deferred();

                return {
                    href: bower_root + '/urth-core-channels/urth-core-channels.html',
                    onload: function() {
                        // Add the global data channel to the document body.
                        var dataChannel = document.createElement('urth-core-channels');
                        dataChannel.setAttribute('id', 'urthChannels');
                        dataChannel.register(dataChannel, '*');
                        dataChannel.addEventListener('connected', function() {
                            deferred.resolve();
                        });
                        document.body.appendChild(dataChannel);

                        // Exposing a global
                        window.UrthData = dataChannel;
                    },
                    promise: deferred.promise()
                };
            })(),
            { href: bower_root + '/urth-core-import/urth-core-import.html' },
            { href: bower_root + '/urth-core-dataframe/urth-core-dataframe.html' },
            { href: bower_root + '/urth-core-function/urth-core-function.html' }
        ];

        links = defaultLinks.concat( links || [] );
        var linksLoaded = 0;

        function updateLinksCompleted() {
            linksLoaded++;
            if (linksLoaded === links.length) {
                // all links loaded; resolve promise
                window.Urth.widgets.whenReady.resolve();
            }
        }

        function linkLoaded(elem, link) {
            if (link.onload) {
                link.onload();
            }
            if (link.promise) {
                link.promise.then(updateLinksCompleted);
            } else {
                updateLinksCompleted();
            }
        }

        // Dynamically add HTML link tags for specified dependencies.
        links.forEach(function(link) {
            if (link.href) {
                var rel = link.rel || 'import';

                var elem = document.createElement('link');
                elem.setAttribute('rel', rel);
                elem.setAttribute('href', link.href);
                elem.onload = function() {
                    linkLoaded(elem, link);
                };

                elem.onerror = function(e) {
                    var msg = 'Failed to load link: ' + link.href;
                    console.error(msg, e);
                    window.Urth.widgets.whenReady.reject(msg);
                };
                document.head.appendChild(elem);
            }
        });
    }

    function loadPolyfill(bower_root, loadHandler, errorHandler) {
        var script = document.createElement('script');
        script.type = 'text/javascript';
        script.setAttribute('src', bower_root+'/webcomponentsjs/webcomponents-lite.min.js');
        script.onload = loadHandler;
        script.onerror = errorHandler;
        document.head.appendChild(script);
    }

    /**
     * Function used to a direct path to bower_components based from where this modules was loaded
     * @return {string}
     */
    function getModuleBasedComponentRoot() {
        var moduleuri = module.uri;
        return moduleuri.substring(0, moduleuri.lastIndexOf('/'))+"/../../bower_components";
    }

    /**
     * Sniff test to see if the server side extensions are available. We are using a well-known element as the test.
     * @param callback
     */
    function isServerExtensionAvailable(callback) {
        urlExist(window.Urth.BASE_URL+'urth_components' + "/urth-core-bind/urth-core-bind.html", callback)
    }

    /**
     * Utility function to check if a URL exist. Callback is invoked with either true or false.
     * @param url
     * @param callback
     */
    function urlExist(url, callback){
        $.ajax({
            type: 'HEAD',
            url: url,
            success: function(){
                callback(true);
            },
            error: function() {
                callback(false);
            }
        });
    }

    return function(baseURL, links) {
        // Enable shadow dom if it is there for polymer elements.
        window.Polymer = window.Polymer || {};
        window.Polymer.dom = 'shadow';

        // Expose the base URL being used.
        window.Urth = window.Urth || {};
        window.Urth.BASE_URL = baseURL;

        // Global promise which is resolved when widgets are fully initialized
        window.Urth.widgets = window.Urth.widgets || {};
        window.Urth.widgets.whenReady = $.Deferred();

        isServerExtensionAvailable(function(isAvailable){
            console.log( "Server extension is " + (isAvailable?"":"NOT ") + "available!");

            //if server extension is available, use the 'urth_components' route, else use a direct path based on this
            //module's uri
            var components_root = isAvailable
                ? baseURL + 'urth_components'
                : getModuleBasedComponentRoot(module)

            loadPolyfill(components_root, function() {
                loadComponents(components_root, links);
            }, function (e) {
                console.error('Failed to load web components polyfill: ' + e);
            });
        });

        return window.Urth.widgets.whenReady;
    };
});
