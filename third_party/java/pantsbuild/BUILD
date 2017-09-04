java_binary(
  name = "jarjar-links",
  main_class = "org.pantsbuild.jarjar.Main",
  licenses = ["notice"], # Apache 2
  runtime_deps = [
    ":jarjar",
  ],
  visibility = [
    "//third_party/java/jetty:__pkg__",
  ],
)

java_import(
  name = "jarjar",
  jars = ["jarjar-1.6.3.jar"],
  srcjar = "jarjar-1.6.3-sources.jar",
  licenses = ["notice"], # Apache 2
  deps = [
    "//third_party/java/ant",
    "//third_party/java/asm",
    "//third_party/java/maven",
  ],
)

