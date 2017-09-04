java_import(
  name = "hamcrest-core",
  jars = ["hamcrest-core-1.3.jar"],
  srcjar = "hamcrest-core-1.3-sources.jar",
  licenses = ["notice"], # BSD
  visibility = [
    "//third_party/java/junit:__pkg__",
  ],
)

java_import(
  name = "hamcrest-library",
  jars = ["hamcrest-library-1.3.jar"],
  srcjar = "hamcrest-library-1.3-sources.jar",
  licenses = ["notice"], # BSD
  deps = [
    ":hamcrest-core",
  ],
  visibility = [
  ],
)
