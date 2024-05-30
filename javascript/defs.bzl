load("//javascript/private:fragment.bzl", _closure_fragment = "closure_fragment")
load("//javascript/private:header.bzl", _closure_lang_file = "closure_lang_file")
load("//javascript/private:mocha_test.bzl", _mocha_test = "mocha_test")
load("//javascript/private:test_suite.bzl", _closure_test_suite = "closure_test_suite")

closure_fragment = _closure_fragment
closure_lang_file = _closure_lang_file
closure_test_suite = _closure_test_suite
mocha_test = _mocha_test

BROWSERS = {
    "chrome": {
        "data": select({
            "@platforms//os:macos": [
                "@mac_chrome//:chrome-js",
                "@mac_chromedriver//:chromedriver-js",
            ],
            "@platforms//os:linux": [
                "@linux_chrome//:chrome-js",
                "@linux_chromedriver//:chromedriver-js",
            ],
        }),
        "env": {
            "SELENIUM_BROWSER": "chrome",
        } | select({
            "@platforms//os:macos": {
                "SELENIUM_BROWSER": "chrome",
                "DRIVER_BINARY": "mac_chromedriver/chromedriver",
                "BROWSER_BINARY": "mac_chrome/Chrome.app/Contents/MacOS/Chrome",
            },
            "@platforms//os:linux": {
                "DRIVER_BINARY": "linux_chromedriver/chromedriver",
                "BROWSER_BINARY": "linux_chrome/chrome-linux64/chrome",
            },
        }),
    },
}
