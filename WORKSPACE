http_archive(
    name = "io_bazel_rules_closure",
    urls = ["http://mirror.bazel.build/github.com/bazelbuild/rules_closure/archive/0.4.1.tar.gz"],
    strip_prefix = "rules_closure-0.4.1",
    sha256 = "ba5e2e10cdc4027702f96e9bdc536c6595decafa94847d08ae28c6cb48225124",
)

load(
  "@io_bazel_rules_closure//closure:defs.bzl",
  "closure_repositories")
closure_repositories()

http_archive(
    name = "io_bazel_rules_docker",
    urls = ["https://github.com/bazelbuild/rules_docker/archive/v0.1.0.tar.gz"],
    strip_prefix = "rules_docker-0.1.0",
    sha256 = "f43bc041d91afc799155543a98962e8dac07146575305b4693552a29367932b7",
)

load(
  "@io_bazel_rules_docker//docker:docker.bzl",
  "docker_repositories", "docker_pull", "docker_build"
)
docker_repositories()

docker_pull(
  name = "java_base",
  registry = "gcr.io",
  repository = "distroless/java",
)

docker_pull(
  name = "cc_base",
  registry = "gcr.io",
  repository = "distroless/cc",
)

#docker_pull(
#    name = "ubuntu-slim",
#    registry = "gcr.io",
#    repository = "google-containers/ubuntu-slim-amd64",
#    digest = "sha256:1d5c0118358fc7651388805e404fe491a80f489bf0e7c5f8ae4156250d6ec7d8",
#)

new_http_archive(
  name = "chromedriver",
  urls = ["https://chromedriver.storage.googleapis.com/2.32/chromedriver_linux64.zip"],
  sha256 = "1e053ebec954790bab426f1547faa6328abff607b246e4493a9d927b1e13d7e4",
  build_file_content = """exports_files(["chromedriver"])"""
)

http_archive(
  name = "distroless",
  urls = ["https://github.com/googlecloudplatform/distroless/archive/b575b433f42e5af9225060f8b02d450168ca4d00.tar.gz"],
  strip_prefix = "distroless-b575b433f42e5af9225060f8b02d450168ca4d00",
  sha256 = "8e0332de730de6755a17f7c3ab641612973cef002109dc6608f5cc05436d955a",
)

load(
    "@distroless//package_manager:package_manager.bzl",
    "package_manager_repositories",
    "dpkg_src",
    "dpkg_list",
)

package_manager_repositories()

dpkg_src(
    name = "debian_jessie",
    arch = "amd64",
    distro = "jessie",
    sha256 = "142cceae78a1343e66a0d27f1b142c406243d7940f626972c2c39ef71499ce61",
    snapshot = "20170821T035341Z",
    url = "http://snapshot.debian.org/archive",
)

# Expands to http://snapshot.debian.org/archive/debian/20170821T035341Z/dists/jessie-backports/main/binary-amd64/Packages.gz
dpkg_src(
    name = "debian_jessie_backports",
    arch = "amd64",
    distro = "jessie-backports",
    sha256 = "eba769f0a0bcaffbb82a8b61d4a9c8a0a3299d5111a68daeaf7e50cc0f76e0ab",
    snapshot = "20170821T035341Z",
    url = "http://snapshot.debian.org/archive",
)

# We want
# http://dl.google.com/linux/chrome/deb/dists/stable/main/binary-amd64/Packages.gz
# http://dl.google.com/linux/chrome/deb/debian/../dists/stable/main/binary-amd64/Packages.gz
# The URL to download from is calculated from:
#url = "%s/debian/%s/dists/%s/main/binary-%s/Packages.gz" % (
#        mirror_url,
#        snapshot,
#        distro,
#        arch
dpkg_src(
    name = "debian_google_chrome",
    arch = "amd64",
    distro = "stable",
    sha256 = "923e3e76fafea1b26df152648e9128d8ba7a92cc43073357e62bb2b33eebc248",
    snapshot = "..",
    url = "http://dl.google.com/linux/chrome/deb",
)

dpkg_list(
    name = "package_bundle",
    packages = [
        # Base OS
        "bash",
        "locales",

        # Java
        "zlib1g",
        "openjdk-8-jre-headless",

    	  # X Windows
        "libx11-6",
        "locales",
        "xvfb",

        # Chrome
        "fontconfig",
        "gconf2",
        "google-chrome-stable",
        "libnss3",
        "libglib2.0-0",
    ],
    sources = [
        "@debian_google_chrome//file:Packages.json",
        "@debian_jessie//file:Packages.json",
        "@debian_jessie_backports//file:Packages.json",
    ],
)
