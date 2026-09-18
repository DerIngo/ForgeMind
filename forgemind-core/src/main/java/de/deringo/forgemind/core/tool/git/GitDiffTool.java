package de.deringo.forgemind.core.tool.git;

import de.deringo.forgemind.core.git.GitExecutor;
import de.deringo.forgemind.core.git.GitResult;
import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GitDiffTool implements AgentTool {

    private final GitExecutor gitExecutor;
    private final ProjectWorkspace workspace;

    public GitDiffTool(
            GitExecutor gitExecutor,
            ProjectWorkspace workspace
    ) {
        this.gitExecutor = gitExecutor;
        this.workspace = workspace;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "git_diff",
                """
                Shows the actual unstaged content changes of tracked files in
                the Git working tree.

                Optionally provide a project-relative path to restrict the diff
                to one file or directory.

                Use this when the user asks what changed in modified tracked
                files.

                Normal git_diff output does not include untracked files.
                Use git_status to identify untracked files and read_file to
                inspect their contents.

                This tool does not modify the repository.
                """,
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description",
                                        "Optional path relative to the project root."
                                )
                        )
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

        List<String> gitArguments =
                new ArrayList<>();

        gitArguments.add("diff");
        gitArguments.add("--");

        Object pathArgument =
                arguments.get("path");

        if (pathArgument instanceof String path
                && !path.isBlank()) {

            workspace.resolve(path);

            gitArguments.add(path);
        }

        GitResult result =
                gitExecutor.execute(
                        gitArguments
                );

        if (!result.successful()) {
            return new ToolResult(
                    "ERROR: git diff failed with exit code "
                            + result.exitCode()
                            + "\n\n"
                            + result.output()
            );
        }

        if (result.output().isBlank()) {
            return new ToolResult(
                    "No unstaged changes."
            );
        }

        return new ToolResult(
                result.output()
        );
    }
}