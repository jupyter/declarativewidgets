/*
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
/**
 * Loads the web component polyfill and all web components specified in
 * elements.html.
 */
define(['require'], function(require) {
    'use strict';

    function loadComponents(bower_root, links) {
        console.debug('Bower root is: ', bower_root);

        var defaultLinks = [
            { href: bower_root + '/urth-core-bind/urth-core-bind.html' },
            {
                href: bower_root + '/urth-core-channels/urth-core-channels.html',
                onload: function(e) {
                    // Add the global data channel to the document body.
                    var dataChannel = document.createElement('urth-core-channels');
                    dataChannel.setAttribute('id', 'urthChannels');
                    dataChannel.register(dataChannel, '*');
                    document.body.appendChild(dataChannel);

                    // Exposing a global
                    window.UrthData = dataChannel;
                }
            },
            { href: bower_root + '/urth-core-import/urth-core-import.html' },
            { href: bower_root + '/urth-core-dataframe/urth-core-dataframe.html' },
            { href: bower_root + '/urth-core-function/urth-core-function.html' }
        ];

        links = defaultLinks.concat( links || [] );

        // Dynamically add HTML link tags for specified dependencies.
        links.forEach(function(link) {
            if (link.href) {
                var rel = link.rel || 'import';

                var elem = document.createElement('link');
                elem.setAttribute('rel', rel);
                elem.setAttribute('href', link.href);
                if (link.onload) {
                    elem.onload = link.onload;
                }

                elem.onerror = function(e) {
                    console.error('Failed to load link: ' + link.href, e);
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

    return function(baseURL, links) {
        // Enable shadow dom if it is there for polymer elements.
        window.Polymer = window.Polymer || {};
        window.Polymer.dom = 'shadow';

        // Expose the base URL being used.
        window.Urth = window.Urth || {};
        window.Urth.BASE_URL = baseURL;

        var bower_root = baseURL + 'urth_components';

        // Load the web components polyfill if it is necessary then
        // load the web components.
        if ('registerElement' in document &&
            'createShadowRoot' in HTMLElement.prototype &&
            'import' in document.createElement('link') &&
            'content' in document.createElement('template')) {
            // Web components fully supported so load the components
            loadComponents(bower_root, links);
        } else {
            // Need to load web components polyfill first.
            loadPolyfill(bower_root, function() {
                loadComponents(bower_root, links);
            }, function (e) {
                console.error('Failed to load web components polyfill: ' + e);
            });
        }
    };
});
