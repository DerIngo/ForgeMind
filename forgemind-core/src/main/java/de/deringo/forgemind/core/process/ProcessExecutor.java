package de.deringo.forgemind.core.process;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ProcessExecutor {

    public ProcessResult execute(
            List<String> command,
            Path workingDirectory,
            Duration timeout
    ) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException(
                    "Command must not be empty."
            );
        }

        if (workingDirectory == null) {
            throw new IllegalArgumentException(
                    "Working directory must not be null."
            );
        }

        if (timeout == null
                || timeout.isZero()
                || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Timeout must be positive."
            );
        }

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        processBuilder.directory(
                workingDirectory.toFile()
        );

        processBuilder.redirectErrorStream(true);

        try {
            Process process =
                    processBuilder.start();

            try (var executor =
                         Executors.newVirtualThreadPerTaskExecutor()) {

                CompletableFuture<String> outputFuture =
                        CompletableFuture.supplyAsync(
                                () -> readOutput(process),
                                executor
                        );

                boolean finished =
                        process.waitFor(
                                timeout.toMillis(),
                                TimeUnit.MILLISECONDS
                        );

                if (!finished) {
                    process.destroyForcibly();
                    process.waitFor();

                    String output =
                            outputFuture.join();

                    return new ProcessResult(
                            -1,
                            output
                    );
                }

                String output =
                        outputFuture.join();

                return new ProcessResult(
                        process.exitValue(),
                        output
                );
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not execute process: "
                            + command.getFirst(),
                    e
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Process execution was interrupted.",
                    e
            );
        }
    }

    private String readOutput(
            Process process
    ) {
        try {
            return new String(
                    process.getInputStream()
                            .readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read process output.",
                    e
            );
        }
    }
}