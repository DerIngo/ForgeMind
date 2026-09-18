package de.deringo.forgemind.core.tool.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

public final class SearchTextTool implements AgentTool {

    private static final int MAX_RESULTS = 50;

    private final ProjectWorkspace workspace;

    public SearchTextTool(ProjectWorkspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public Capability capability() {
        return Capability.READ;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "search_text",
                """
                Searches recursively for project files whose path or file name
                contains the given text, case-insensitively.

                The query is plain text, not a glob or regular expression.
                Do not use wildcards such as * or ?.
                """,
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description",
                                        "Case-sensitive text to search for."
                                )
                        ),
                        "required", new String[]{"query"}
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {

        String query = (String) arguments.get("query");

        List<String> matches = new ArrayList<>();

        try (var paths = workspace.walk()) {

            for (Path path : paths
                    .filter(Files::isRegularFile)
                    .filter(workspace::isTextFile)
                    .toList()) {

                searchFile(path, query, matches);

                if (matches.size() >= MAX_RESULTS) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not search project.",
                    e
            );
        }

        if (matches.isEmpty()) {
            return new ToolResult("No matches found.");
        }

        return new ToolResult(
                String.join("\n", matches)
        );
    }

    private void searchFile(
            Path file,
            String query,
            List<String> matches
    ) {

        try {
            List<String> lines = Files.readAllLines(file);

            for (int i = 0; i < lines.size(); i++) {

                if (lines.get(i).contains(query)) {

                    String relative =
                            workspace.relative(file).toString();

                    matches.add(
                            relative
                                    + ":"
                                    + (i + 1)
                                    + ": "
                                    + lines.get(i).trim()
                    );

                    if (matches.size() >= MAX_RESULTS) {
                        return;
                    }
                }
            }

        } catch (IOException ignored) {
            // Binary/unreadable files are ignored for now.
        }
    }
}