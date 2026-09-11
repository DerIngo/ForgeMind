package de.deringo.forgemind.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ForgeMindCliTest {

    @Test
    void startsSuccessfullyAndPrintsGreetingFromCore() throws Exception {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(
                javaExecutable, "-cp", System.getProperty("java.class.path"), ForgeMindCli.class.getName())
                .start();
        try {
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "CLI did not terminate");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), errors);
            assertEquals("Hello World!" + System.lineSeparator(), output);
            assertEquals("", errors);
        } finally {
            process.destroyForcibly();
        }
    }
}
