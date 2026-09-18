package de.deringo.forgemind.core.tool.filesystem;

import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Collectors;

import de.deringo.forgemind.core.permission.Capability;
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
    public Capability capability() {
        return Capability.READ;
    }
    
    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "list_files",
                """
                Lists the immediate contents of a directory.
                
                Use this only when the directory contents themselves are needed.
                Do not use this tool to recursively discover project structure or
                locate files. Use search_files for file discovery.
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
