package de.deringo.forgemind.core.git;

public record GitResult(
        int exitCode,
        String output
) {

    public boolean successful() {
        return exitCode == 0;
    }
}