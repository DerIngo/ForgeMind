package de.deringo.forgemind.cli;

import de.deringo.forgemind.core.tool.ToolCall;
import de.deringo.forgemind.core.tool.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("java.lang.System.out")
class ConsoleAgentObserverTest {
    private String render(String tool, String content) {
        var bytes = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (var capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            new ConsoleAgentObserver().onToolResult(new ToolCall("1", tool, Map.of()),
                    new ToolResult(content), Duration.ZERO);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test void successfulLongCommandAlwaysShowsExitCode() {
        String output = render("run_command", "Exit code: 0\n\nOutput:\n" + "build output".repeat(100));
        assertTrue(output.contains("Exit code: 0"));
        assertFalse(output.contains("build output"));
    }

    @Test void failedLongCommandShowsCompleteOutput() {
        String content = "Exit code: 1\r\n\r\nOutput:\r\n" + "failure details\n".repeat(2000) + "LAST ERROR";
        assertTrue(render("run_command", content).contains(content));
    }

    @Test void permissionDenialIsVisible() {
        assertTrue(render("write_file", "Permission denied by user.").contains("Permission denied by user."));
        assertTrue(render("write_file", "Permission denied by policy.").contains("Permission denied by policy."));
    }
}
