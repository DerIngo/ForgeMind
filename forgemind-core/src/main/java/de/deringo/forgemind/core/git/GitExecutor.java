package de.deringo.forgemind.core.git;

import de.deringo.forgemind.core.process.ProcessExecutor;
import de.deringo.forgemind.core.process.ProcessResult;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class GitExecutor {

    private static final Duration DEFAULT_TIMEOUT =
            Duration.ofSeconds(30);

    private final ProjectWorkspace workspace;
    private final ProcessExecutor processExecutor;

    public GitExecutor(
            ProjectWorkspace workspace,
            ProcessExecutor processExecutor
    ) {
        this.workspace = workspace;
        this.processExecutor = processExecutor;
    }

    public GitResult execute(
            List<String> arguments
    ) {
        if (arguments == null) {
            throw new IllegalArgumentException(
                    "Git arguments must not be null."
            );
        }

        List<String> command =
                new ArrayList<>();

        command.add("git");
        command.addAll(arguments);

        ProcessResult result =
                processExecutor.execute(
                        command,
                        workspace.root(),
                        DEFAULT_TIMEOUT
                );

        return new GitResult(
                result.exitCode(),
                result.output()
        );
    }
}