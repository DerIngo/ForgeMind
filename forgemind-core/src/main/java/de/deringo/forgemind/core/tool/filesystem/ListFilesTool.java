package de.deringo.forgemind.core.tool.filesystem;

import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Collectors;

import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

public final class ListFilesTool implements AgentTool {

    private final ProjectWorkspace workspace;

    public ListFilesTool(ProjectWorkspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "list_files",
                """
                Lists the immediate contents of one directory.

                Do not use this tool to navigate source trees one level at a time.
                When searching for code, classes, or implementations, use search_files first.
                """,
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description",
                                        "Directory relative to the project root. Use '.' for the project root."
                                )
                        ),
                        "required", new String[]{"path"}
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {

        String path = (String) arguments.get("path");

        String result = workspace.list(path)
                .stream()
                .map(entry -> {
                    String relative =
                            workspace.relative(entry).toString();

                    return Files.isDirectory(entry)
                            ? "[DIR]  " + relative
                            : "[FILE] " + relative;
                })
                .collect(Collectors.joining("\n"));

        return new ToolResult(result);
    }
}
