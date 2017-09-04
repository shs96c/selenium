load("@io_bazel_rules_docker//docker:docker.bzl", "docker_build")
load("@distroless//package_manager:package_manager.bzl", "dpkg_list")
load("@package_bundle//file:packages.bzl", "packages")

java_binary(
  name = "selenium",
  main_class = "org.openqa.grid.selenium.GridLauncherV3",
  srcs = glob([
    "*.java",
    "proxy/*.java",
  ]),
  deps = [
    "//java/client/src/org/openqa/selenium/chrome",
    "//java/client/src/org/openqa/selenium/firefox",
    "//java/client/src/org/openqa/selenium/remote",
    "//java/server/src/org/openqa/grid:grid",
    "//java/server/src/org/openqa/selenium/remote/server",
    "//java/server/src/org/openqa/selenium/remote/server/log",
    "//third_party/java/beust:jcommander",
    "//third_party/java/guava",
    "//third_party/java/servlet:servlet-api",
  ],
)

docker_build(
    name = "java-base",
#    base = "@ubuntu-slim//image",
    base = "@cc_base//image",
    debs = [
        packages["zlib1g"],
        packages["openjdk-8-jre-headless"],
    ],
    symlinks = {
        "/usr/bin/java": "/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java",
    },
)

docker_build(
    name = "hub",
    base = ":java-base",
    directory = "/opt/selenium",
    files = [":selenium_deploy.jar"],
    cmd = ["/usr/bin/java", "-jar", "/opt/selenium/selenium_deploy.jar", "-role", "hub"],
)

docker_build(
    name = "node",
    base = ":java-base",
    directory = "/opt/selenium",
    files = [":selenium_deploy.jar"],
    debs = [
        packages["libx11-6"],
        packages["locales"],
        packages["xvfb"],
    ],
)

docker_build(
    name = "chrome-node",
    base = ":node",
    directory = "/usr/bin",
    files = [
        "@chromedriver//:chromedriver",
    ],
    debs = [
        packages["fontconfig"],
        packages["gconf2"],
        packages["google-chrome-stable"],
        packages["libglib2.0-0"],
    ],
    cmd = ["/usr/bin/xvfb-run", "--server-args", "-screen 0 1360x1020x24 -ac +extension RANDR",
           "/usr/bin/java", "-jar", "/opt/selenium/selenium_deploy.jar", "-role", "node"],
)
