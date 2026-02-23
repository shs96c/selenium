"""Rule for copying an executable target to a new path."""

def _copy_binary_impl(ctx):
    out = ctx.actions.declare_file(ctx.attr.out)
    ctx.actions.symlink(output = out, target_file = ctx.executable.src)
    return [DefaultInfo(files = depset([out]))]

copy_binary = rule(
    doc = "Copy an executable target to a new path. Unlike copy_file, this works " +
          "with multi-file targets like platform_data and keeps the target in the " +
          "target configuration (not exec), so rustc optimization flags apply.",
    implementation = _copy_binary_impl,
    attrs = {
        "src": attr.label(executable = True, cfg = "target", mandatory = True),
        "out": attr.string(mandatory = True),
    },
)
