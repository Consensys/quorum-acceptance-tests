var cfg = require("./config")
var assert = require('assert')

/*
test to ensure config parameters are defined
 */
describe("config", function () {

    describe("accounts", function () {
        it('should have accounts defined', function () {
            assert.equal(8, cfg.accounts().length)
        })
    })

    describe("nodes", function () {
        it('should have nodes defined', function () {
            var p = 22000
            assert.equal(cfg.nodesToTest() + 1, cfg.nodes().length)
            for (var k = 1; k <= cfg.nodesToTest(); ++k) {
                assert.equal(cfg.nodes()[k], 'http://localhost:' + p)
                p++
            }
        })
    })

    describe("constellation", function () {
        it('should have keys defined', function () {
            assert.equal(8, cfg.constellations().length)
        })
    })

})