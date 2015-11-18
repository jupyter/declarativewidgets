//options:
// baseurl - base url for notebook server to test
// server - the selenium server, defaults to ondemand.saucelabs.com
// platform - defaults to 'OS X 10.10'
// browser - defaults to 'chrome'
// verbose

var wd = require('wd');
require('colors');
var chai = require('chai');
var chaiAsPromised = require('chai-as-promised');
var parseArgs = require('minimist');
var args = parseArgs(process.argv);

chai.use(chaiAsPromised);
chai.should();
chaiAsPromised.transferPromiseness = wd.transferPromiseness;

// http configuration, not needed for simple runs
wd.configureHttp({
    timeout: 60000,
    retryDelay: 15000,
    retries: 5,
    baseUrl: args.baseurl
});

var desired = {browserName: 'chrome', platform: 'OS X 10.10'};
desired.platform = args.platform || desired.platform;
desired.browserName = args.browser || desired.browserName;
desired.name = 'Urth System Test with ' + desired.browserName;
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
        return this.waitForElementByCssSelector(
            browserSupportsShadowDOM ? 'urth-viz-table::shadow .handsontable' : 'urth-viz-table .handsontable',
            wd.asserters.isDisplayed,
            timeout)
        .catch(tagChaiAssertionError);
    };

    before(function(done) { 
            // http://user:apiKey@ondemand.saucelabs.com/wd/hub
        var auth = args['sauce-username'] && args['sauce-access-key'] ?
            args['sauce-username'] + ':' + args['sauce-access-key'] + '@' : '';
        console.log('http://' + auth + (args.server || 'ondemand.saucelabs.com') + '/wd/hub');
        browser = wd.promiseChainRemote('http://' + auth + (args.server || 'ondemand.saucelabs.com') + '/wd/hub');

        if (args.verbose) {
            // optional logging
            browser.on('status', function(info) {
                console.log(info.cyan);
            });
            browser.on('command', function(meth, path, data) {
                console.log(' > ' + meth.yellow, path.grey, data || '');
            });
        }

        browser
            .init(desired)
            .get('/notebooks/tests/urth-viz-table.ipynb')
            .sleep(5000)
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
        var result = browser
            .quit();
        // if (result.sauceJobStatus) {
        //     result = result.sauceJobStatus(allPassed);
        // }
        result.nodeify(done);
    });

    it('should run all cells and find a handsontable in the 3rd output area', function(done) {
        browser
            .waitForElementsByCssSelector('div.output_area').nth(3)
            .waitForWidgetElement("urth-viz-table", browserSupportsShadowDOM, 10000)
            .nodeify(done);
    });

});
