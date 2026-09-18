package de.deringo.forgemind.core.tool.filesystem;


import java.util.Map;

import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

public final class ReadFileTool implements AgentTool {

    private final ProjectWorkspace workspace;

    public ReadFileTool(ProjectWorkspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "read_file",
                "Reads a text file relative to the project root.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description",
                                        "Path relative to the project root."
                                )
                        ),
                        "required", new String[]{"path"}
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {

        String path = (String) arguments.get("path");

        return new ToolResult(
                workspace.readFile(path)
        );
    }
}