var wd = require('wd');
require('colors');
var _ = require('lodash');
var chai = require('chai');
var chaiAsPromised = require('chai-as-promised');

chai.use(chaiAsPromised);
chai.should();
chaiAsPromised.transferPromiseness = wd.transferPromiseness;

// checking sauce credential
if (!process.env.SAUCE_USERNAME || !process.env.SAUCE_ACCESS_KEY) {
    console.warn(
        '\nPlease configure your sauce credential:\n\n' +
        'export SAUCE_USERNAME=<SAUCE_USERNAME>\n' +
        'export SAUCE_ACCESS_KEY=<SAUCE_ACCESS_KEY>\n\n'
    );
    throw new Error('Missing sauce credentials');
}

// http configuration, not needed for simple runs
wd.configureHttp({
    timeout: 60000,
    retryDelay: 15000,
    retries: 5
});

var desired = JSON.parse(process.env.DESIRED || '{"browserName": "chrome"}');
desired.name = 'example with ' + desired.browserName;
desired.tags = ['tutorial'];

var Asserter = wd.Asserter;

describe('system-test (' + desired.browserName + ')', function() {
    var browser;
    var allPassed = true;
    var browserSupportsShadowDOM;

    var tagChaiAssertionError = function(err) {
        // throw error and tag as retriable to poll again
        err.retriable = err instanceof chai.AssertionError;
        throw err;
    };

    wd.PromiseChainWebdriver.prototype.waitForWidgetElement = function(selector, browserSupportsShadowDOM, timeout, pollFreq) {
        if (browserSupportsShadowDOM) {
            return this.waitForElementByCssSelector('urth-viz-table::shadow .handsontable', wd.asserters.isDisplayed, 5000)
                    .catch(tagChaiAssertionError);
        } else {
            return this.waitForElementByCssSelector('urth-viz-table .handsontable', wd.asserters.isDisplayed, 5000)
                    .catch(tagChaiAssertionError);
        }
    };

    before(function(done) {
        var username = process.env.SAUCE_USERNAME;
        var accessKey = process.env.SAUCE_ACCESS_KEY;
        browser = wd.promiseChainRemote();//'ondemand.saucelabs.com', 80, username, accessKey);
        if (process.env.VERBOSE) {
            // optional logging
            browser.on('status', function(info) {
                console.log(info.cyan);
            });
            browser.on('command', function(meth, path, data) {
                console.log(' > ' + meth.yellow, path.grey, data || '');
            });
        }
        console.log("before Hook");
        browser
            .init(desired)
            .get('http://jupyter.cloudet.xyz/user/r4TF1CcUxpDR/notebooks/widgets/examples/urth-viz-chart.ipynb')
            .waitForElementByLinkText("Cell", wd.asserters.isDisplayed, 10000)
            .elementByLinkText("Cell")
            .click()
            .waitForElementByLinkText("Run All", wd.asserters.isDisplayed, 10000)
            .elementByLinkText("Run All")
            .click()
            .eval("!!document.body.createShadowRoot", function(err, value) {
                browserSupportsShadowDOM = value;
                done();
            });
    });

    afterEach(function(done) {
        allPassed = allPassed && (this.currentTest.state === 'passed');
        done();
    });

    after(function(done) {
        console.log("after Hook");
        browser
            .quit()
            // .sauceJobStatus(allPassed)
            .nodeify(done);
    });

    it('should run all cells and find a handsontable in the 3rd output area', function(done) {
        browser
            .waitForElementsByCssSelector('div.output_area').nth(3)
            .waitForWidgetElement("urth-viz-table", browserSupportsShadowDOM, 10000)
            .nodeify(done);

    });

});
