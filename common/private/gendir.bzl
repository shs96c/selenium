def _gendir_impl(ctx):
    out = ctx.actions.declare_directory(ctx.attr.out)

    command = ctx.expand_location(ctx.attr.cmd)

    command = command.replace("$@", out.path)

    ctx.actions.run_shell(
        outputs = [out],
        inputs = ctx.files.srcs,
        tools = ctx.files.tools,
        command = ctx.attr.cmd,
    )

    return DefaultInfo(
        files = depset([out]),
    )

gendir = rule(
    _gendir_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "out": attr.string(mandatory = True),
        "cmd": attr.string(mandatory = True),
        "tools": attr.label_list(
            allow_files = True,
            cfg = "exec",
        ),
    },
)
