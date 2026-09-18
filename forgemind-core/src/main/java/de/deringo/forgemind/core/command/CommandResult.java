package de.deringo.forgemind.core.command;

public record CommandResult(
        int exitCode,
        String output
) {
}