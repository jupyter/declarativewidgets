// Copyright (c) Jupyter Development Team.
// Distributed under the terms of the Modified BSD License.

var wd = require('wd');
var Boilerplate = require('./utils/boilerplate');
var boilerplate = new Boilerplate();

process.env.PYTHON != "python2" && describe('Widgets Scala System Test', function() {
    boilerplate.setup(this.title, '/notebooks/tests/Walkthrough-Scala.ipynb', 11);

    var timeout = 30000;

    it('should print the correct variable that is used for urth-core-function', function(done) {

        boilerplate.browser
            .elementsByCssSelector('div.code_cell').nth(5)
            .elementByXPath('//button[text()="invoke"]').click()
            .waitForElementById('test1', wd.asserters.textInclude('world'), timeout)
            .nodeify(done);
    });

    it('should bind variable to channel a', function(done) {
        boilerplate.browser
            .elementsByCssSelector('div.code_cell').nth(6)
            .elementByCssSelector('>', 'input')
            .type('A')
            .waitForElementById('test2', wd.asserters.textInclude('A'), timeout)
            .nodeify(done);
    });

    it('should bind variable to channel b', function(done) {
        boilerplate.browser
            .elementsByCssSelector('div.code_cell').nth(7)
            .elementByCssSelector('>', 'input')
            .type('B')
            .waitForElementById('test3', wd.asserters.textInclude('B'), timeout)
            .nodeify(done);
    });

    it('should bind variables to channels independently', function(done) {
        boilerplate.browser
            .elementsByCssSelector('div.code_cell').nth(6)
            .elementByCssSelector('>', 'input')
            .type('2')
            .elementByCssSelector('#test2')
            .text().should.eventually.include('A2')
            .waitForElementById('test2', wd.asserters.textInclude('A2'), timeout)
            .waitForElementById('test3', wd.asserters.textInclude('B'), timeout)
            .nodeify(done);
    });

    it('should watch for changes in a watched variable', function(done) {
        boilerplate.browser
            .elementByXPath('//button[text()="initChannelWatch"]').click()
            .elementsByCssSelector('div.code_cell').nth(8)
            .elementByCssSelector('>', 'input')
            .type('watched message')
            .waitForElementById('test4', wd.asserters.textInclude('watched message'), timeout)
            .nodeify(done);
    });

    it('should update output when DataFrame is modified and set to auto', function(done) {
        boilerplate.browser
            .elementsByCssSelector('div.code_cell').nth(14)
            .waitForElementByClassName('test5', wd.asserters.textInclude('Richard Roe'), timeout)
            .nodeify(done);
    });
});