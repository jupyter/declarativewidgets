/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
/* globals console:false */
/**
 * Loads the web component polyfill and all web components specified in
 * elements.html.
 */
define([
    'require',
    'jquery',
    '../widgets/DeclWidgetModel'], function(require, $) {
    'use strict';

    function loadComponents(bower_root, links) {
        console.debug('Bower root is: ', bower_root);

        var defaultLinks = [
            { href: bower_root + '/urth-core-bind/urth-core-bind.html' },
            (function() {
                var deferred = $.Deferred();

                return {
                    href: bower_root + '/urth-core-channel/urth-core-channel-broker.html',
                    onload: function() {
                        // Add the global data channel to the document body.
                        var broker = window.Urth['urth-core-channel-broker'].getChannelBroker();
                        broker.addEventListener('connected', function() {
                            deferred.resolve();
                        });

                        // Exposing a global
                        window.UrthData = broker;
                    },
                    promise: deferred.promise()
                };
            })(),
            { href: bower_root + '/urth-core-channel/urth-core-channel.html' },
            { href: bower_root + '/urth-core-channel/urth-core-channel-item.html' },
            { href: bower_root + '/urth-core-import/urth-core-import.html' },
            { href: bower_root + '/urth-core-dataframe/urth-core-dataframe.html' },
            { href: bower_root + '/urth-core-function/urth-core-function.html' },
            { href: bower_root + '/urth-core-storage/urth-core-storage.html' },
            { href: bower_root + '/urth-core-watch/urth-core-watch.html' }
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

    return function(baseURL, config) {
        // Enable shadow dom if it is there for polymer elements.
        window.Polymer = window.Polymer || {};
        window.Polymer.dom = 'shadow';

        // Expose the base URL being used.
        window.Urth = window.Urth || {};
        window.Urth.BASE_URL = baseURL;

        // Global promise which is resolved when widgets are fully initialized
        window.Urth.widgets = window.Urth.widgets || {};
        window.Urth.widgets.whenReady = $.Deferred();

        var bower_root = baseURL + 'urth_components';

        // expose suppressErrors, false by default to display errors
        window.Urth.suppressErrors = config && config.suppressErrors;

        loadPolyfill(bower_root, function() {
            var links = config && config.links;
            loadComponents(bower_root, links);
        }, function (e) {
            console.error('Failed to load web components polyfill: ' + e);
        });

        return window.Urth.widgets.whenReady;
    };
});
