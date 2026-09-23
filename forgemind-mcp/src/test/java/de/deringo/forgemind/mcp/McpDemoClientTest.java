package de.deringo.forgemind.mcp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class McpDemoClientTest {
    @TempDir Path temp;

    @ParameterizedTest
    @ValueSource(strings = {"", "ForgeMind"})
    void runsCompleteClientServerRoundTrip(String name) throws Exception {
        try (var server = ForgeMindMCP.start("127.0.0.1", 0)) {
            Path output = temp.resolve("client.log");
            var command = new ArrayList<>(List.of(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("java.class.path"), McpDemoClient.class.getName(), "http://127.0.0.1:" + server.port()));
            if (!name.isEmpty()) {
                command.add(name);
            }
            var process = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(output.toFile()).start();
            try {
                assertTrue(process.waitFor(25, TimeUnit.SECONDS), "Demo client did not terminate");
                String log = Files.readString(output);
                assertEquals(0, process.exitValue(), log);
                assertTrue(log.contains("Connected to: forgemind-mcp"), log);
                assertTrue(log.contains("- hello:"), log);
                assertTrue(log.contains("Hello, " + (name.isEmpty() ? "World" : name) + "!"), log);
                assertTrue(log.contains("Connection closed."), log);
            } finally {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
            }
    }
}
