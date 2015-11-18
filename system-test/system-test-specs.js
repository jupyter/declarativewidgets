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
desired.tunnelIdentifier = process.env.TRAVIS_JOB_NUMBER;
desired.platform = args.platform || desired.platform;
desired.browserName = args.browser || desired.browserName;
desired.name = 'Urth System Test with ' + desired.browserName;
desired.tags = ['tutorial'];

var Asserter = wd.Asserter;

describe('system-test (' + desired.browserName + ')', function() {
    var browser;
    var allPassed = true;
    var browserSupportsShadowDOM;

    before(function(done) { 
            // http://user:apiKey@ondemand.saucelabs.com/wd/hub
        console.log('http://' + (args.server || 'ondemand.saucelabs.com') + '/wd/hub');
        browser = wd.promiseChainRemote('http://' + (args.server || 'ondemand.saucelabs.com') + '/wd/hub');

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
            .get('/notebooks/tests/Walkthrough.ipynb')
            .sleep(300000)
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

    it('should prints the correct variable that is used for urth-core-function', function(done) {
        browser
            .sleep(5000)
            .elementsByCssSelector('<', 'div.output_area').nth(2)
            .elementByXPath('//button[text()="invoke"]').click()
            .sleep(5000)
            .elementsByCssSelector('<', 'div.output_area').nth(2)
            .elementByXPath('//span[@id="test1"]')
            .text().then(function(txt) {
                console.log("span test1 is: ", txt);
                txt.should.include('world');
            })
            .nodeify(done);
    });

    it('should bind variable to channel a', function(done) {
        browser
            .elementsByCssSelector('div.output_area').nth(3)
            .elementByCssSelector('>', 'input')
            .type('A')
            .elementsByCssSelector('<', 'div.output_area').nth(3)
            .elementByXPath('//span[@id="test2"]')
            .text().then(function(txt) {
                console.log("span test2 is: ", txt);
                txt.should.include('A');
            })
            .nodeify(done);
    });

    it('should bind variable to channel b', function(done) {
        browser
            .elementsByCssSelector('div.output_area').nth(4)
            .elementByCssSelector('>', 'input')
            .type('B')
            .elementsByCssSelector('<', 'div.output_area').nth(4)
            .elementByXPath('//span[@id="test3"]')
            .text().then(function(txt) {
                console.log("span test3 is: ", txt);
                txt.should.include('B');
            })
            .nodeify(done);
    });

    it('should bind variables to channels independently', function(done) {
        browser
            .elementsByCssSelector('div.output_area').nth(3)
            .elementByCssSelector('>', 'input')
            .type('2')
            .elementsByCssSelector('<', 'div.output_area').nth(3)
            .elementByXPath('//span[@id="test2"]')
            .text().then(function(txt) {
                console.log("span test2 is: ", txt);
                txt.should.include('A2');
            })
            .elementsByCssSelector('<', 'div.output_area').nth(4)
            .elementByXPath('//span[@id="test3"]')
            .text().then(function(txt) {
                console.log("span test3 is: ", txt);
                txt.should.include('B');
            })
            .nodeify(done);
    });

    it('should watch for changes in a watched variable', function(done) {
        browser
            .elementsByCssSelector('div.output_area').nth(5)
            .elementByCssSelector('>', 'input')
            .type('watched message')
            .sleep(3000)
            .elementsByCssSelector('<', 'div.output_area').nth(6)
            .elementByXPath('//span[@id="test4"]')
            .text().then(function(txt) {
                console.log("span test4 is: ", txt);
                txt.should.include('watched message');
            })
            .nodeify(done);
    });

    it('should update output when DataFrame is modified and set to auto', function(done) {
        browser
            .elementsByCssSelector('div.input').nth(10)
            .click()
            .sleep(3000)
            .elementByLinkText("<", "Cell")
            .click()
            .waitForElementByLinkText("Run", wd.asserters.isDisplayed, 10000)
            .elementByLinkText("Run")
            .click()
            .sleep(3000)
            .elementsByCssSelector('<', 'div.output_area').nth(7)
            .elementByXPath('//span[@class="test5"]')
            .text().then(function(txt) {
                console.log("span test5 is: ", txt);
                txt.should.include('Jane Doe');
            })
            .nodeify(done);
    });
});
