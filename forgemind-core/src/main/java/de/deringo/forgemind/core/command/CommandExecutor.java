package de.deringo.forgemind.core.command;

import de.deringo.forgemind.core.process.ProcessExecutor;
import de.deringo.forgemind.core.process.ProcessResult;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CommandExecutor {

    private static final Duration DEFAULT_TIMEOUT =
            Duration.ofMinutes(2);

    private static final boolean WINDOWS =
            System.getProperty("os.name")
                    .toLowerCase()
                    .contains("win");

    private final ProjectWorkspace workspace;
    private final ProcessExecutor processExecutor;

    public CommandExecutor(
            ProjectWorkspace workspace,
            ProcessExecutor processExecutor
    ) {
        this.workspace = workspace;
        this.processExecutor = processExecutor;
    }

    public CommandResult execute(
            List<String> command,
            String workingDirectory
    ) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException(
                    "Command must not be empty."
            );
        }

        validateCommand(command);

        List<String> normalizedCommand =
                normalizeCommand(command);

        ProcessResult result =
                processExecutor.execute(
                        normalizedCommand,
                        workspace.resolve(workingDirectory),
                        DEFAULT_TIMEOUT
                );

        return new CommandResult(
                result.exitCode(),
                result.output()
        );
    }

    private void validateCommand(
            List<String> command
    ) {
        for (String argument : command) {
            if (argument == null) {
                throw new IllegalArgumentException(
                        "Command arguments must not be null."
                );
            }
        }

        if (command.getFirst().isBlank()) {
            throw new IllegalArgumentException(
                    "Command executable must not be blank."
            );
        }
    }

    private List<String> normalizeCommand(
            List<String> command
    ) {
        if (!WINDOWS) {
            return List.copyOf(command);
        }

        String executable =
                command.getFirst();

        String windowsExecutable =
                switch (executable) {
                    case "./mvnw", "mvnw" ->
                            "mvnw.cmd";
                    case "mvn" ->
                            "mvn.cmd";
                    default ->
                            null;
                };

        if (windowsExecutable == null) {
            return List.copyOf(command);
        }

        List<String> normalized =
                new ArrayList<>();

        normalized.add("cmd.exe");
        normalized.add("/c");
        normalized.add(windowsExecutable);

        normalized.addAll(
                command.subList(
                        1,
                        command.size()
                )
        );

        return normalized;
    }
}