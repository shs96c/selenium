'use strict'

const assert = require('node:assert')
// const promise = require('selenium-webdriver/lib/promise')
// const { Browser, By, error, withTagName, until } = require('selenium-webdriver')
// const { Pages, ignore, suite, whereIs } = require('./lib/test')
// const { locateWith } = require('selenium-webdriver/lib/by')
// const { RelativeBy } = require('selenium-webdriver')
const driverFactory = require('./driver_factory')

describe('running a browser', function () {
  this.timeout(6000000)

  describe('here we go', function() {
    let driver

    before(function() {
      driver = driverFactory.GetBrowserForTests()
    })

    it('can do something', function() {
      driver.quit()
    })
  })
})
