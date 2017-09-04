java_library(
    name = "grid",
    srcs = glob([
        "common/**/*.java",
        "internal/**/*java",
        "shared/*.java",
        "web/**/*.java",
    ]),
    deps = [
        "//java/client/src/org/openqa/selenium/net",
        "//java/client/src/org/openqa/selenium/remote",
        "//java/server/src/org/openqa/selenium/remote/server:common",
        "//java/server/src/org/openqa/selenium/remote/server/log",
        "//third_party/java/beust:jcommander",
        "//third_party/java/gson",
        "//third_party/java/guava",
        "//third_party/java/httpcomponents:httpclient",
        "//third_party/java/jcip_annotations",
        "//third_party/java/jetty:jetty-repack",
        "//third_party/java/servlet:servlet-api",
    ],
    visibility = [
        "//java/server/src/org/openqa:__subpackages__",
    ],
)
