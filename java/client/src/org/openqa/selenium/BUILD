java_library(
  name = "selenium",
  exports = [
    ":core-api",
    "//java/client/src/org/openqa/selenium/html5",
    "//java/client/src/org/openqa/selenium/interactions",
    "//java/client/src/org/openqa/selenium/mobile",
  ],
  visibility = [
    "//visibility:public",
  ],
)

java_library(
  name = "beta",
  srcs = ["Beta.java"],
  deps = [
    # Deliberately empty
  ],
  visibility = [
    "//java/client/src/org/openqa/selenium/logging:__pkg__",
  ],
)

# This library is used to construct as much of
# what we export from "selenium" as possible.
java_library(
  name = "core-api",
  srcs = glob([
    "*.java",
    "*Exception.java",
    "internal/*.java",
    "security/*.java",
  ], exclude = ["Beta.java"]),
  exports = [
    ":beta",
    "//java/client/src/org/openqa/selenium/logging",
  ],
  deps = [
    ":beta",
    "//java/client/src/org/openqa/selenium/logging",
  ],
  visibility = [
    "//java/client/src/org/openqa/selenium/interactions:__pkg__"
  ],
)
