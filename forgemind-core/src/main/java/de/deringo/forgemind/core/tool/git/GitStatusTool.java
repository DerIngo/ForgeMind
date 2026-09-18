package de.deringo.forgemind.core.tool.git;

import de.deringo.forgemind.core.git.GitExecutor;
import de.deringo.forgemind.core.git.GitResult;
import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolResult;

import java.util.List;
import java.util.Map;

public final class GitStatusTool implements AgentTool {

    private final GitExecutor gitExecutor;

    public GitStatusTool(
            GitExecutor gitExecutor
    ) {
        this.gitExecutor = gitExecutor;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "git_status",
                """
                Shows which files are modified, added, deleted, renamed, or
                untracked in the current Git working tree.

                This tool shows file status only. It does not show the actual
                content changes.

                When the user asks what changed, use git_diff after git_status
                for tracked modified files. Read untracked files separately
                because they are not included in normal git_diff output.

                This tool does not modify the repository.
                """,
                Map.of(
                        "type", "object",
                        "properties", Map.of()
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

        GitResult result =
                gitExecutor.execute(
                        List.of(
                                "status",
                                "--short",
                                "--untracked-files=all"
                        )
                );

        if (!result.successful()) {
            return new ToolResult(
                    "ERROR: git status failed with exit code "
                            + result.exitCode()
                            + "\n\n"
                            + result.output()
            );
        }

        if (result.output().isBlank()) {
            return new ToolResult(
                    "Working tree is clean."
            );
        }

        return new ToolResult(
                result.output()
        );
    }
}