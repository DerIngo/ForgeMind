package de.deringo.forgemind.core.tool.filesystem;

import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ListFilesTool implements AgentTool {

    private final Path projectRoot;

    public ListFilesTool(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "list_files",
                "Lists files and directories in a directory relative to the project root.",
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

        String pathArgument = (String) arguments.get("path");

        Path directory = projectRoot
                .resolve(pathArgument)
                .normalize();

        if (!directory.startsWith(projectRoot)) {
            throw new IllegalArgumentException(
                    "Access outside project root is not allowed: "
                            + pathArgument
            );
        }

        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException(
                    "Not a directory: " + pathArgument
            );
        }

        try (var stream = Files.list(directory)) {

            String result = stream
                    .sorted()
                    .map(path -> {
                        String relative =
                                projectRoot.relativize(path).toString();

                        return Files.isDirectory(path)
                                ? "[DIR]  " + relative
                                : "[FILE] " + relative;
                    })
                    .reduce(
                            new StringBuilder(),
                            (builder, value) ->
                                    builder.append(value).append('\n'),
                            StringBuilder::append
                    )
                    .toString();

            return new ToolResult(result);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not list directory: " + pathArgument,
                    e
            );
        }
    }
}
