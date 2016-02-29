# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.
from IPython.core.display import display, Javascript

def init():
    # JavaScript code to load the declarative widgets extension.
    # Code sent to the front end from here may be executed after
    # extension initialization (iterative cell execution) or
    # before (Run All, Reload). This code works together with the
    # extension initialization to ensure that the required API is
    # available in all scenarios.
    #
    # Urth._initialized is a deferred that is resolved by the extension
    # initialization after the global Urth instance has been setup.
    # If extension initialization has not completed a new deferred is
    # initialized which extension initialization will resolve.
    #
    # Urth.whenReady is a public API defined by extension initialization
    # to delay javascript execution until dependencies have loaded. If
    # extension initialization has not completed a wrapper implementation
    # is setup which will invoke the real implementation when it is available.
    code = '''
        window.Urth = window.Urth || {};
        Urth._initialized = Urth._initialized || $.Deferred();
        Urth.whenReady = Urth.whenReady || function(cb) {
            Urth._initialized.then(function() {
                Urth.whenReady(cb);
            });
        };
        Urth.whenReady(function() { console.log("Declarative widgets connected.") });
        '''

    # Send the code to the browser.
    display(Javascript(code))
