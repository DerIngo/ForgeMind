package de.deringo.forgemind.core.tool.filesystem;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;

public final class ReadFileTool implements AgentTool {

    private final Path projectRoot;

    public ReadFileTool(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
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
                                        "description", "Path relative to the project root"
                                )
                        ),
                        "required", new String[]{"path"}
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {

        String pathArgument = (String) arguments.get("path");

        Path path = projectRoot
                .resolve(pathArgument)
                .normalize();

        if (!path.startsWith(projectRoot)) {
            throw new IllegalArgumentException(
                    "Access outside project root is not allowed: " + pathArgument
            );
        }

        try {
            return new ToolResult(Files.readString(path));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read file: " + pathArgument,
                    e
            );
        }
    }
}