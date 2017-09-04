java_library(
  name = "remote",
  srcs = glob([
    "*.java",
    "html5/*.java",
    "http/*.java",
    "internal/*.java",
    "mobile/*.java",
    "service/*.java",
  ]),
  exports = [
    "//java/client/src/org/openqa/selenium",
  ],
  deps = [
    "//java/client/src/org/openqa/selenium",
    "//java/client/src/org/openqa/selenium/io",
    "//java/client/src/org/openqa/selenium/net",
    "//java/client/src/org/openqa/selenium/os",
    "//third_party/java/bytebuddy",
    "//third_party/java/httpcomponents:httpclient",
    "//third_party/java/gson",
    "//third_party/java/guava",
  ],
  visibility = [
    "//visibility:public",
  ],
)
