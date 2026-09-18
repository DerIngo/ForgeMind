package de.deringo.forgemind.core.process;

public record ProcessResult(
        int exitCode,
        String output
) {

    public boolean successful() {
        return exitCode == 0;
    }
}