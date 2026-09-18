package de.deringo.forgemind.core.tool.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;
import de.deringo.forgemind.core.util.GlobMatcher;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

public final class SearchFilesTool implements AgentTool {

    private static final int MAX_RESULTS = 100;

    private final ProjectWorkspace workspace;

    public SearchFilesTool(ProjectWorkspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "search_files",
                """
                Searches recursively for files in the project.

                The query can be either:

                - Plain text for a case-insensitive substring search.
                  Example: AgentLoop

                - A glob pattern using * or ?.
                  Examples:
                  *Test*.java
                  **/*Test.java
                  **/pom.xml
                  forgemind-core/**/*.java

                A glob without a directory component is matched recursively
                against file names throughout the project.

                Use the most specific query possible.
                """,
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description",
                                        "File name, path fragment, or glob pattern."
                                )
                        ),
                        "required",
                        new String[]{"query"}
                )
        );
    }

    @Override
    public Capability capability() {
        return Capability.READ;
    }

    @Override
    public ToolResult execute(
            Map<String, Object> arguments
    ) {

        String query = (String) arguments.get("query");

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query must not be blank."
            );
        }

        boolean glob = containsGlobCharacters(query);

        String normalizedQuery =
                query.toLowerCase(Locale.ROOT);

        List<String> matches = new ArrayList<>();

        try (var paths = workspace.walk()) {

            paths.filter(Files::isRegularFile)
                    .map(workspace::relativeString)
                    .filter(path ->
                            glob
                                    ? GlobMatcher.matches(query, path)
                                    : path.toLowerCase(Locale.ROOT)
                                            .contains(normalizedQuery)
                    )
                    .limit(MAX_RESULTS + 1L)
                    .forEach(matches::add);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not search project files.",
                    e
            );
        }

        if (matches.isEmpty()) {
            return new ToolResult("No files found.");
        }

        boolean truncated =
                matches.size() > MAX_RESULTS;

        if (truncated) {
            matches = new ArrayList<>(
                    matches.subList(0, MAX_RESULTS)
            );
        }

        String result =
                String.join(
                        System.lineSeparator(),
                        matches
                );

        if (truncated) {
            result += System.lineSeparator()
                    + System.lineSeparator()
                    + "Showing first "
                    + MAX_RESULTS
                    + " matches. More files exist; refine the query.";
        }

        return new ToolResult(result);
    }

    private boolean containsGlobCharacters(
            String query
    ) {
        return query.indexOf('*') >= 0
                || query.indexOf('?') >= 0;
    }
}