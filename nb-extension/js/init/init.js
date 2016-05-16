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
    'module',
    'jquery'
], function(module, $) {
    'use strict';

    // Enable shadow dom if it is there for polymer elements.
    window.Polymer = window.Polymer || {};
    window.Polymer.dom = 'shadow';

    // Some versions of IE do not have window.console defined. Some versions
    // do not define the debug and other methods. This is a minimal workaround
    // based on what declarative widgets code is using.
    window.console = window.console || {};
    window.console.log = window.console.log || function () {
        };
    ['debug', 'error', 'trace', 'warn'].forEach(function (method) {
        window.console[method] = window.console[method] || window.console.log;
    });

    var COMPONENTS_DIR = 'urth_components';

    function loadComponents(components_root, links) {
        console.debug('Components root is: ', components_root);
        var whenLoaded = $.Deferred();

        var defaultLinks = [
            { href: components_root + '/urth-core-bind/urth-core-bind.html' },
            { href: components_root + '/urth-core-channel/urth-core-channel-broker.html' },
            { href: components_root + '/urth-core-channel/urth-core-channel.html' },
            { href: components_root + '/urth-core-channel/urth-core-channel-item.html' },
            { href: components_root + '/urth-core-import/urth-core-import.html' },
            { href: components_root + '/urth-core-dataframe/urth-core-dataframe.html' },
            { href: components_root + '/urth-core-function/urth-core-function.html' },
            { href: components_root + '/urth-core-storage/urth-core-storage.html' },
            { href: components_root + '/urth-core-watch/urth-core-watch.html' },
            { href: components_root + '/urth-help/urth-help.html' }
        ];

        links = defaultLinks.concat( links || [] );
        var linksLoaded = 0;

        function updateLinksCompleted() {
            linksLoaded++;
            if (linksLoaded === links.length) {
                // all links loaded; resolve promise
                whenLoaded.resolve();
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
                    whenLoaded.reject(msg);
                };
                document.head.appendChild(elem);
            }
        });

        return whenLoaded;
    }

    function loadPolyfill(components_root, loadHandler, errorHandler) {
        var script = document.createElement('script');

        // Invoke the load handler when the polyfill has completed
        // initialization.
        window.addEventListener('WebComponentsReady', loadHandler);

        script.type = 'text/javascript';
        script.setAttribute('src', components_root+'/webcomponentsjs/webcomponents-lite.min.js');
        script.onerror = errorHandler;
        document.head.appendChild(script);
    }

    /**
     * Sniff test to see if the server side extensions are available.
     * We are using a well-known element as the test.
     * @param callback
     */
    function isServerExtensionAvailable(components_root, callback) {
        // Request jupyter-notebook-env.html since it is a small file with
        // no dependencies.
        urlExist(components_root + '/urth-core-behaviors/jupyter-notebook-env.html', callback);
    }

    /**
     * Utility function to check if a URL exist. Callback is invoked with either true or false.
     * @param url
     * @param callback
     */
    function urlExist(url, callback){
        // Using a `GET` request instead of a `HEAD` request because the `HEAD`
        // request was causing system test failures on Sauce Labs.
        $.ajax({
            type: 'GET',
            url: url,
            success: function(){
                callback(true);
            },
            error: function() {
                callback(false);
            }
        });
    }

    /**
     * Initialize Declarative Widgets.
     *
     * @param  {Object} config - configuration; see following entries for more information
     * @param  {Object} config.namespace - Jupyter Notebook namespace object (or shim)
     * @param  {Object} config.events - Notebook events object
     * @param  {Object} config.suppressErrors - Set to true to disable error message output.
     */
    var DeclWidgets = function(config) {
        // Global deferred which is resolved when widgets are fully initialized.
        // Deferred may have been initialized already by kernel code injected
        // into the page.
        this._initialized = this._initialized || $.Deferred();

        if (typeof config === 'string') {
            // backwards compatibility with old API, which took `baseURL` as first argument and
            // optional `config` as second arg
            this._baseURL = config;
            this._config = arguments[1] || {};
        } else {
            this._baseURL = config.namespace ? config.namespace.notebook.base_url : '/';
            this._config = config;
        }

        // expose suppressErrors, false by default to display errors
        this.suppressErrors = config.suppressErrors;

        this.events = config.events;

        // specify a getter for the kernel instance, since it can be restarted and a new kernel
        // instantiated
        Object.defineProperty(this, 'kernel', {
            get: function () {
                // TODO What is the correct way of handling this outside of the notebook? What
                //      should we do when using jupyter-js-services and kernel is restarted?
                return this._config.namespace.notebook.kernel;
            }
        });
    }

    /**
     * Inherit any predefined Urth properties if they exist.
     */
    DeclWidgets.prototype = Object.create(window.Urth || {});

    /**
     * Load the prerequisites for the extension and connect to the kernel.
     *
     * @return {Promise} Resolved when the extension has connected to the kernel.
     */
    DeclWidgets.prototype.connect = function() {
        if (this._whenConnected) {
            return this._whenConnected;
        }

        var whenConnected = $.Deferred();
        this._whenConnected = whenConnected.promise();

        isServerExtensionAvailable(this._baseURL + COMPONENTS_DIR, function (isAvailable) {
            console.log('Server extension is ' + (isAvailable ? '' : 'NOT ') + 'available!');

            // If server extension is available, use the baseURL route, else
            // use a direct path based on this module's uri.
            var components_root = isAvailable
                ? this._baseURL
                : getModuleBasedComponentRoot(module);

            this.BASE_URL = components_root;
            components_root += COMPONENTS_DIR;

            // Load the polyfill and the required components then listen for
            // kernel connection. Need to load polyfill first because loading
            // the components first produced inconsistent timing across browsers.
            loadPolyfill(components_root, function () {
                loadComponents(components_root, this._config.links).then(function() {
                    // Add the global data channel to the document body.
                    var broker = window.Urth['urth-core-channel-broker'].getChannelBroker();

                    // If the kernel has already connected then resolve the
                    // promise, otherwise wait for the `connected` event.
                    if (this.kernel && this.kernel.is_connected()) {
                        whenConnected.resolve();
                    } else {
                        broker.addEventListener('connected', function() {
                            whenConnected.resolve();
                        });
                    }
                }.bind(this)).fail(function() {
                    console.error('Failed to load required components.')
                    whenConnected.reject();
                });
            }.bind(this), function (e) {
                console.error('Failed to load web components polyfill: ' + e);
                whenConnected.reject();
            });
        }.bind(this));

        return this._whenConnected;
    };

    /**
     * Invokes the specified function when prerequisites have finished loading.
     *
     * @param {Function} Function to invoke.
     */
    DeclWidgets.prototype.whenReady = function(cb) {
        if (typeof cb !== 'function') {
            return;
        }
        this.connect().then(function() {
            var importBroker = Urth['urth-core-import-broker'].getImportBroker();
            var pendingImports = importBroker.getPendingImports().map(function(key) {
                return key.completed;
            });
            var pendingImports = Promise.all(pendingImports);
            pendingImports.then(cb);
        });
    };

    /**
     * Function used to a get direct path from where this modules was loaded.
     *
     * @return {string} Module path.
     */
    DeclWidgets.prototype._getModuleBasedComponentRoot = function() {
        var moduleuri = module.uri;
        return moduleuri.substring(0, moduleuri.lastIndexOf('/')) + '/../../';
    }

    var the_declwidgets;
    var getOrCreate = function(config){
        if (!the_declwidgets) {
            window.Urth = the_declwidgets = new DeclWidgets(config);
            the_declwidgets._initialized.resolve();
        }
        return the_declwidgets;
    }

    return getOrCreate;
});
