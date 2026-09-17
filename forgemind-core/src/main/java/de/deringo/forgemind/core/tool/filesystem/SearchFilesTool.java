package de.deringo.forgemind.core.tool.filesystem;

import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public final class SearchFilesTool implements AgentTool {

    private static final int MAX_RESULTS = 100;

    private final Path projectRoot;

    public SearchFilesTool(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "search_files",
                """
                Recursively searches the entire project for matching file names and paths.
                
                Use this as the first tool when locating a class, implementation, component,
                package, or source file. Use the most specific available search term.
                """,
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description",
                                        "Case-insensitive filename or path fragment to search for."
                                )
                        ),
                        "required", new String[]{"query"}
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {

        String query = ((String) arguments.get("query"))
                .toLowerCase();

        try (var stream = Files.walk(projectRoot)) {

            String result = stream
                    .filter(Files::isRegularFile)
                    .map(projectRoot::relativize)
                    .filter(path ->
                            path.toString()
                                    .toLowerCase()
                                    .contains(query)
                    )
                    .limit(MAX_RESULTS)
                    .map(Path::toString)
                    .collect(Collectors.joining("\n"));

            if (result.isBlank()) {
                return new ToolResult("No files found.");
            }

            return new ToolResult(result);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not search project files.",
                    e
            );
        }
    }
}
