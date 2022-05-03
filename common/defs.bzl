load("@bazel_skylib//rules:copy_file.bzl", _copy_file = "copy_file")
load("//common/private:gendir.bzl", _gendir = "gendir")
load("//common/private:zip_file.bzl", _zip_file = "zip_file")

copy_file = _copy_file
gendir = _gendir
zip_file = _zip_file
