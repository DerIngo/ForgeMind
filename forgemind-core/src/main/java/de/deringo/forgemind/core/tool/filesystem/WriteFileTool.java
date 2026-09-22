package de.deringo.forgemind.core.tool.filesystem;

import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.permission.PermissionKey;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

import java.util.Map;

public final class WriteFileTool implements AgentTool {

    private final ProjectWorkspace workspace;

    public WriteFileTool(
            ProjectWorkspace workspace
    ) {
        this.workspace = workspace;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "write_file",
                """
                Creates a new text file in the project.

                The path must be relative to the project root.
                Supply the complete module/source/package path, for example
                module/src/test/java/com/example/ExampleTest.java.
                A bare filename creates the file directly in the project root;
                package declarations do not determine the destination directory.
                Follow the existing source layout and extend existing tests
                instead of creating standalone debug files in the project root.
                Missing parent directories are created automatically.

                This tool only creates new files. It never overwrites
                an existing file. Use replace_text or apply_patch to modify existing
                files.
                """,
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description",
                                        "Path relative to the project root."
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description",
                                        "Complete content of the new file."
                                )
                        ),
                        "required",
                        new String[]{
                                "path",
                                "content"
                        }
                )
        );
    }

    @Override
    public Capability capability() {
        return Capability.WRITE;
    }

    @Override
    public PermissionKey permissionKey(
            Map<String, Object> arguments
    ) {
        return new PermissionKey(
                Capability.WRITE,
                definition().name(),
                (String) arguments.get("path")
        );
    }

    @Override
    public ToolResult execute(
            Map<String, Object> arguments
    ) {
        String path =
                (String) arguments.get("path");

        String content =
                (String) arguments.get("content");

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "Path must not be blank."
            );
        }

        if (content == null) {
            throw new IllegalArgumentException(
                    "Content must not be null."
            );
        }

        workspace.createFile(
                path,
                content
        );

        return new ToolResult(
                "Created file: " + path
        );
    }
}
