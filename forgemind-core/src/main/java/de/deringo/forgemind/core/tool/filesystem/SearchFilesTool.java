package de.deringo.forgemind.core.tool.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

public final class SearchFilesTool implements AgentTool {

    private static final int MAX_RESULTS = 100;

    private final ProjectWorkspace workspace;

    public SearchFilesTool(ProjectWorkspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public Capability capability() {
        return Capability.READ;
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

        try (var paths = workspace.walk()) {

            String result = paths
                    .filter(Files::isRegularFile)
                    .map(workspace::relative)
                    .filter(path ->
                            path.toString()
                                    .toLowerCase()
                                    .contains(query)
                    )
                    .limit(MAX_RESULTS)
                    .map(Path::toString)
                    .collect(Collectors.joining("\n"));

            return new ToolResult(
                    result.isBlank()
                            ? "No files found."
                            : result
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not search project files.",
                    e
            );
        }
    }
}
