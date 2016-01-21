// Copyright (c) Jupyter Development Team.
// Distributed under the terms of the Modified BSD License.

var wd = require('wd');
var chai = require('chai');
var Boilerplate = require('./utils/boilerplate');
var boilerplate = new Boilerplate();

describe('Urth Viz Table Test', function() {

    var tagChaiAssertionError = function(err) {
        // throw error and tag as retriable to poll again
        err.retriable = err instanceof chai.AssertionError;
        throw err;
    };

    wd.PromiseChainWebdriver.prototype.waitForWidgetElement = function(selector, browserSupportsShadowDOM, timeout, pollFreq) {
        return this.waitForElementByCssSelector(
            browserSupportsShadowDOM ? 'urth-viz-table::shadow .handsontable' : 'urth-viz-table .handsontable',
            wd.asserters.isDisplayed,
            timeout)
        .catch(tagChaiAssertionError);
    };

    boilerplate.setup(this.title, '/notebooks/tests/urth-viz-table.ipynb', 3);

    it('should run all cells and find a handsontable in the 3rd output area', function(done) {
        boilerplate.browser
            .waitForElementsByCssSelector('div.output_area').nth(3)
            .waitForWidgetElement("urth-viz-table", boilerplate.browserSupportsShadowDOM, 10000)
            .nodeify(done);
    });
});
