// Copyright (c) Jupyter Development Team.
// Distributed under the terms of the Modified BSD License.

var wd = require('wd');
var Boilerplate = require('./utils/boilerplate');
var boilerplate = new Boilerplate();

process.env.PYTHON != "python2" && describe('Widgets R System Test', function() {
    boilerplate.setup(this.title, '/notebooks/tests/urth-r-widgets.ipynb');

    it('should print the result of a Function Widget invocation', function(done) {
        boilerplate.browser
            .waitForElementByClassName('test1', wd.asserters.textInclude('10'), 60000)
            .nodeify(done);
    });

    it('should bind and update a channel variable', function(done) {
        boilerplate.browser
            .waitForElementById('invokeButton', wd.asserters.isDisplayed, 60000)
            .click()
            .click() // Safari requires extra click for some reason
            .waitForElementByClassName('test2', wd.asserters.textInclude('mike'), 60000)
            .nodeify(done);
    });

    it('should print the contents of the Dataframe Widget', function(done) {
        boilerplate.browser
            .waitForElementByClassName('test3', wd.asserters.textInclude('John'), 60000)
            .nodeify(done);
    });

    it('should print the contents of the SparkDataframe Widget', function(done) {
        boilerplate.browser
            .waitForElementByClassName('test4', wd.asserters.textInclude('John'), 60000)
            .nodeify(done);
    });
});
