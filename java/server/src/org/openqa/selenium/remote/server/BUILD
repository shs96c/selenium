java_library(
  name = "common",
  srcs = [
    "ActiveSession.java",
    "ActiveSessionCommandExecutor.java",
    "ActiveSessionFactory.java",
    "ActiveSessions.java",
    "ActiveSessionListener.java",
    "CapabilitiesComparator.java",
    "CommandHandler.java",
    "DefaultDriverFactory.java",
    "DefaultDriverProvider.java",
    "DefaultDriverSessions.java",
    "DefaultSession.java",
    "DriverFactory.java",
    "DriverProvider.java",
    "DriverServlet.java",
    "DriverSessions.java",
    "InMemorySession.java",
    "InputStreamWrappingServletInputStream.java",
    "JsonHttpCommandHandler.java",
    "JsonParametersAware.java",
    "KnownElements.java",
    "NewSessionPayload.java",
    "Passthrough.java",
    "ProtocolConverter.java",
    "ServicedSession.java",
    "ServletRequestWrappingHttpRequest.java",
    "ServletResponseWrappingHttpResponse.java",
    "Session.java",
    "SessionCodec.java",
    "SessionFactory.java",
    "SnapshotScreenListener.java",
    "StaticResourceHandler.java",
    "TeeReader.java",
  ] + glob([
    "handler/**/*.java",
    "rest/*.java",
    "xdrpc/*.java",
  ]),
  exports = [
    "//java/server/src/org/openqa/selenium/remote/server/log",
  ],
  deps = [
    "//java/client/src/org/openqa/selenium/io",
    "//java/client/src/org/openqa/selenium/net",
    "//java/client/src/org/openqa/selenium/remote",
    "//java/client/src/org/openqa/selenium/support",
    "//java/server/src/org/openqa/selenium/remote/server/log",
    "//third_party/java/gson",
    "//third_party/java/guava",
    "//third_party/java/servlet:servlet-api",
  ],
  visibility = [
    "//java/server/src/org/openqa/grid:__pkg__",
  ],
)

java_library(
  name = "server",
  srcs = [
    "SeleniumServer.java",
  ],
  deps = [
    ":common",
    ":webdriver-servlet",
    "//java/client/src/org/openqa/selenium/remote",
    "//java/server/src/org/openqa/grid",
    "//third_party/java/beust:jcommander",
    "//third_party/java/jetty:jetty-repack",
    "//third_party/java/servlet:servlet-api",
  ],
  visibility = [
    "//java/server/src/org/openqa/grid/selenium:__pkg__",
  ],
)

java_binary(
  name = "server-standalone",
  main_class = "org.openqa.selenium.remote.server.SeleniumServer",
  runtime_deps = [
    ":server",
    "//java/client/src/org/openqa/selenium/chrome",
    "//java/client/src/org/openqa/selenium/edge",
    "//java/client/src/org/openqa/selenium/firefox",
    "//java/client/src/org/openqa/selenium/ie",
    "//java/client/src/org/openqa/selenium/opera",
    "//java/client/src/org/openqa/selenium/safari",
  ],
)

java_library(
  name = "webdriver-servlet",
  srcs = [
    "AllHandlers.java",
    "UrlTemplate.java",
    "WebDriverServlet.java",
  ] + glob([
    "commandhandler/*.java",
  ]),
  deps = [
    ":common",
    "//java/client/src/org/openqa/selenium/io",
    "//java/client/src/org/openqa/selenium/remote",
    "//third_party/java/guava",
    "//third_party/java/servlet:servlet-api",
  ],
)