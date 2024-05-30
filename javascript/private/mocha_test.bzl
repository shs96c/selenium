load("@npm//javascript/node/selenium-webdriver:mocha/package_json.bzl", mocha_bin = "bin")

_TIMEOUTS = {
    "small": "60000",
    "medium": "300000",
    "large": "900000",
}

def mocha_test(name, args = [], data = [], env = {}, size = None, **kwargs):
    args = [
        "--timeout",
        _TIMEOUTS.get(size, "60000"),
    ] + args

    env = {
        # Add environment variable so that mocha writes its test xml
        # to the location Bazel expects.
        "MOCHA_FILE": "$$XML_OUTPUT_FILE",
    } | env

    data = [
        "//javascript/node/selenium-webdriver:node_modules/mocha-junit-reporter",
    ] + data

    mocha_bin.mocha_test(
        name = name,
        args = args,
        data = data,
        env = env,
        **kwargs
    )
