// Copyright (c) Jupyter Development Team.
// Distributed under the terms of the Modified BSD License.

var wd = require('wd');
var Boilerplate = require('./utils/boilerplate');
var boilerplate = new Boilerplate();

describe('Urth Core Bind', function() {

    boilerplate.setup(this.title, '/notebooks/tests/urth-core-bind.ipynb');

    it('should run all cells and wait for dependency to load', function(done) {
        // Using a random number to protect against the possibility of a previous
        // test run value having been persisted.
        var inputString = 'Hello ' + Math.random();

        boilerplate.browser
            .waitForElementById('titleInput', wd.asserters.isDisplayed, 10000)
            .elementById('titleInput')
            .click()
            .keys(inputString)
            .elementById('titleSpan')
            .text().should.eventually.include(inputString)
            .nodeify(done);
    });
});
