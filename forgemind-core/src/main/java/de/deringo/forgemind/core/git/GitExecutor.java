package de.deringo.forgemind.core.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.deringo.forgemind.core.workspace.ProjectWorkspace;

public final class GitExecutor {

    private static final Duration DEFAULT_TIMEOUT =
            Duration.ofSeconds(30);

    private final ProjectWorkspace workspace;

    public GitExecutor(ProjectWorkspace workspace) {
        this.workspace = workspace;
    }

    public GitResult execute(List<String> arguments) {

        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(arguments);

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        processBuilder.directory(
                workspace.root().toFile()
        );

        processBuilder.redirectErrorStream(true);

        try {

            Process process =
                    processBuilder.start();

            try (var executor =
                    Executors.newVirtualThreadPerTaskExecutor()) {

           CompletableFuture<String> outputFuture =
                   CompletableFuture.supplyAsync(
                           () -> {
                               try {
                                   return new String(
                                           process.getInputStream()
                                                   .readAllBytes(),
                                           StandardCharsets.UTF_8
                                   );
                               } catch (IOException e) {
                                   throw new IllegalStateException(e);
                               }
                           },
                           executor
                   );

           boolean finished =
                   process.waitFor(
                           DEFAULT_TIMEOUT.toMillis(),
                           TimeUnit.MILLISECONDS
                   );

           if (!finished) {
               process.destroyForcibly();
               process.waitFor();

               return new GitResult(
                       -1,
                       "ERROR: Git command timed out after "
                               + DEFAULT_TIMEOUT
               );
           }

           String output =
                   outputFuture.join();

           return new GitResult(
                   process.exitValue(),
                   output
           );
       }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Could not execute Git.",
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Git execution was interrupted.",
                    e
            );
        }
    }
}