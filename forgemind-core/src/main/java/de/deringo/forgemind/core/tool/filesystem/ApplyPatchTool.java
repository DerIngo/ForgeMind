package de.deringo.forgemind.core.tool.filesystem;

import de.deringo.forgemind.core.patch.FilePatch;
import de.deringo.forgemind.core.patch.PatchApplier;
import de.deringo.forgemind.core.patch.PatchParser;
import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.permission.PermissionKey;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ApplyPatchTool implements AgentTool {

    private final PatchParser parser;
    private final PatchApplier applier;

    public ApplyPatchTool(
            PatchParser parser,
            PatchApplier applier
    ) {
        this.parser = parser;
        this.applier = applier;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "apply_patch",
                """
                Applies focused changes to existing text files.

                Use this for modifying existing files when replace_text
                would be inconvenient or when multiple nearby lines need
                to change.

                The patch format is:

                *** Begin Patch
                *** Update File: path/to/File.java
                @@
                 unchanged context
                -old line
                +new line
                 unchanged context
                *** End Patch

                Lines beginning with a space are unchanged context.
                Lines beginning with - are removed.
                Lines beginning with + are added.

                Multiple @@ hunks and multiple files may be included.

                This is a strict custom format, not GNU unified diff.
                Start with *** Begin Patch and always finish with *** End Patch
                on its own line inside the patch string.
                Hunk headers must be exactly @@, without line numbers or labels.
                Every line inside a hunk needs a prefix, including empty lines:
                use a single space for empty context, + for an added empty line,
                and - for a removed empty line. Preserve indentation after the prefix.
                Send only the focused change, not a copy of the entire file.
                If parsing fails, correct the reported format error and resend the
                complete patch. Do not use write_file to overwrite an existing file.

                Each hunk must match the current file exactly and
                unambiguously.
                Match complete lines, and include existing context for additions.

                This tool only modifies existing files. Use write_file
                to create new files.
                """,
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "patch", Map.of(
                                        "type", "string",
                                        "description",
                                        "Patch describing changes to existing files."
                                )
                        ),
                        "required",
                        new String[]{
                                "patch"
                        }
                )
        );
    }

    @Override
    public void validateArguments(Map<String, Object> arguments) {
        parser.parse(patchText(arguments));
    }

    private String patchText(Map<String, Object> arguments) {
        if (!(arguments.get("patch") instanceof String patch)) {
            throw new IllegalArgumentException("Argument 'patch' must be a string containing a complete patch.");
        }
        return patch;
    }

    @Override
    public Capability capability() {
        return Capability.WRITE;
    }

    @Override
    public PermissionKey permissionKey(
            Map<String, Object> arguments
    ) {
        String patchText = arguments.get("patch") instanceof String value ? value : null;

        if (patchText == null) {
            return new PermissionKey(
                    Capability.WRITE,
                    definition().name(),
                    "patch"
            );
        }

        try {
            List<FilePatch> patches =
                    parser.parse(patchText);

            String resource =
                    patches.stream()
                            .map(FilePatch::path)
                            .sorted()
                            .collect(
                                    Collectors.joining(", ")
                            );

            return new PermissionKey(
                    Capability.WRITE,
                    definition().name(),
                    resource
            );

        } catch (RuntimeException e) {

            /*
             * The actual tool execution will report the parsing
             * error. Permission-key generation should not hide it.
             */
            return new PermissionKey(
                    Capability.WRITE,
                    definition().name(),
                    "invalid-patch"
            );
        }
    }

    @Override
    public ToolResult execute(
            Map<String, Object> arguments
    ) {
        String patchText = patchText(arguments);

        if (patchText == null
                || patchText.isBlank()) {
            throw new IllegalArgumentException(
                    "Patch must not be blank."
            );
        }

        List<FilePatch> patches =
                parser.parse(patchText);

        applier.apply(patches);

        String changedFiles =
                patches.stream()
                        .map(FilePatch::path)
                        .distinct()
                        .collect(
                                Collectors.joining(
                                        System.lineSeparator()
                                )
                        );

        return new ToolResult(
                "Patch applied successfully to:"
                        + System.lineSeparator()
                        + changedFiles
        );
    }
}
