JETTY_VERSION = '9.4.5.v20170502'

java_library(
  name = "jetty-repack",
  licenses = ["notice"], # Apache 2
  exports = [
    ":jetty-continuation-repacked",
    ":jetty-http-repacked",
    ":jetty-io-repacked",
    ":jetty-jmx-repacked",
    ":jetty-security-repacked",
    ":jetty-server-repacked",
    ":jetty-servlet-repacked",
    ":jetty-servlets-repacked",
    ":jetty-util-repacked",
  ],
  runtime_deps = [
    ":jetty-continuation-repacked",
    ":jetty-http-repacked",
    ":jetty-io-repacked",
    ":jetty-jmx-repacked",
    ":jetty-security-repacked",
    ":jetty-server-repacked",
    ":jetty-servlet-repacked",
    ":jetty-servlets-repacked",
    ":jetty-util-repacked",
    "//third_party/java/servlet:servlet-api",
  ],
  visibility = [
    "//java/server/src/org/openqa:__subpackages__",
  ],
)

[
  genrule(
    name = "%s-repacked" % i,
    outs = ["%s-repacked.jar" % i],
    licenses = ["notice"], # Apache 2
    cmd = "$(location //third_party/java/pantsbuild:jarjar-links) process $(location jetty-repack-rules) $(location :%s-%s.jar) $@" % (i, JETTY_VERSION),
    srcs = [
      "%s-%s.jar" % (i, JETTY_VERSION),
      "jetty-repack-rules",
    ],
    tools = [
      "//third_party/java/pantsbuild:jarjar-links",
    ],
  )
  for i in [
    "jetty-continuation",
    "jetty-http",
    "jetty-io",
    "jetty-jmx",
    "jetty-security",
    "jetty-server",
    "jetty-servlet",
    "jetty-servlets",
    "jetty-util",
  ]
]


#REPACK = [
#  "jetty-http",
#]
#for i in REPACK:
#  out = '%s-repacked' % i
#  name = '%s-do-repack' % i
#
#  genrule(
#    name = out,
#    out = "%s.jar" % out,
#    cmd = "$(location //third_party/java/pantsbuild:jarjar-links) process $(location :jarjar-rules) $(location :%s) $@" % i
#  )
