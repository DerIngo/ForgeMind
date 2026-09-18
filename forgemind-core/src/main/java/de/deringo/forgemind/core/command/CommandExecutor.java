package de.deringo.forgemind.core.command;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.deringo.forgemind.core.workspace.ProjectWorkspace;

public final class CommandExecutor {

    private static final boolean WINDOWS =
            System.getProperty("os.name")
                    .toLowerCase()
                    .contains("win");
    
    private static final Duration DEFAULT_TIMEOUT =
            Duration.ofMinutes(2);

    private final ProjectWorkspace workspace;

    public CommandExecutor(ProjectWorkspace workspace) {
        this.workspace = workspace;
    }

    public CommandResult execute(
            List<String> command,
            String workingDirectory
    ) {
        Path directory = workspace.resolve(workingDirectory);

        validateCommand(command);
        
        List<String> preparedCommand =
                prepareCommand(command);

        ProcessBuilder processBuilder =
                new ProcessBuilder(preparedCommand);

        processBuilder.directory(directory.toFile());

        // Keep Maven/compiler output in its original order.
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

             // Read output asynchronously using a virtual thread executor
             CompletableFuture<String> outputFuture;
             try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                 outputFuture = CompletableFuture.supplyAsync(() -> {
                     try {
                         return new String(
                                 process.getInputStream().readAllBytes(),
                                 StandardCharsets.UTF_8
                         );
                     } catch (IOException e) {
                         throw new UncheckedIOException(e);
                     }
                 }, executor);
             }
    
             boolean finished = process.waitFor(
                     DEFAULT_TIMEOUT.toMillis(),
                     TimeUnit.MILLISECONDS
             );
    
             if (!finished) {
                 process.destroyForcibly();
                 process.waitFor();
                 outputFuture.cancel(true);
    
                 return new CommandResult(
                         -1,
                         "ERROR: Command timed out after " + DEFAULT_TIMEOUT
                 );
             }
    
             // Blocks until the stream read completes and returns the String
             String output = outputFuture.join();
    
             return new CommandResult(
                     process.exitValue(),
                     output
             ); 

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not execute command: " + command,
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Command execution interrupted.",
                    e
            );
        }
    }
    
    private List<String> prepareCommand(List<String> command) {

        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException(
                    "Command must not be empty."
            );
        }

        if (command.getFirst() == null
                || command.getFirst().isBlank()) {
            throw new IllegalArgumentException(
                    "Executable must not be empty."
            );
        }

        if (!WINDOWS) {
            return command;
        }

        String executable = command.getFirst();

        if (executable.equals("./mvnw")
                || executable.equals("mvnw")) {

            List<String> result = new ArrayList<>();

            result.add("cmd.exe");
            result.add("/c");
            result.add("mvnw.cmd");
            result.addAll(command.subList(1, command.size()));

            return result;
        }

        if (executable.equals("mvn")) {

            List<String> result = new ArrayList<>();

            result.add("cmd.exe");
            result.add("/c");
            result.add("mvn.cmd");
            result.addAll(command.subList(1, command.size()));

            return result;
        }

        return command;
    }
    
    private void validateCommand(List<String> command) {

        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException(
                    "Command must contain an executable."
            );
        }

        if (command.getFirst() == null
                || command.getFirst().isBlank()) {
            throw new IllegalArgumentException(
                    "Command executable must not be blank."
            );
        }

        for (String argument : command) {
            if (argument == null) {
                throw new IllegalArgumentException(
                        "Command arguments must not contain null."
                );
            }
        }
    }
}