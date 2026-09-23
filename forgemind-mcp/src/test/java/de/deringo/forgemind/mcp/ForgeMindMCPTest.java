package de.deringo.forgemind.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.TypeRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ForgeMindMCPTest {
    @TempDir Path temp;

    @Test
    @Timeout(40)
    void servesHelloOverStdioAndExitsOnEof() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var command = new ArrayList<>(List.of(java));
        String jar = System.getProperty("mcp.test.jar");
        command.addAll(jar == null
                ? List.of("-cp", System.getProperty("java.class.path"), ForgeMindMCP.class.getName())
                : List.of("-jar", jar));
        Path errors = temp.resolve("stderr.log");
        Process process = new ProcessBuilder(command).redirectError(errors.toFile()).start();
        try (var reader = process.inputReader(StandardCharsets.UTF_8);
             var writer = process.outputWriter(StandardCharsets.UTF_8);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            send(writer, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1.0\"}}}");
            var initialized = response(reader, executor);
            assertEquals("forgemind-mcp", ((Map<?, ?>) result(initialized).get("serverInfo")).get("name"));
            send(writer, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
            send(writer, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}");
            var tools = (List<?>) result(response(reader, executor)).get("tools");
            assertEquals(1, tools.size());
            var tool = (Map<?, ?>) tools.getFirst();
            assertEquals("hello", tool.get("name"));
            assertEquals(List.of("name"), ((Map<?, ?>) tool.get("inputSchema")).get("required"));
            send(writer, "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"hello\",\"arguments\":{\"name\":\"Welt\"}}}");
            var greeting = result(response(reader, executor));
            assertEquals(false, greeting.get("isError"));
            assertEquals("Hello, Welt!", ((Map<?, ?>) ((List<?>) greeting.get("content")).getFirst()).get("text"));
            for (String arguments : List.of("{}", "{\"name\":42}", "{\"name\":\"\"}")) {
                send(writer, "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"hello\",\"arguments\":" + arguments + "}}");
                assertEquals(true, result(response(reader, executor)).get("isError"));
            }
            writer.close();
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Server did not exit on EOF");
            assertEquals(0, process.exitValue(), Files.readString(errors));
        } finally {
            process.destroyForcibly();
        }
    }

    private void send(BufferedWriter writer, String message) throws IOException {
        writer.write(message);
        writer.newLine();
        writer.flush();
    }

    private Map<String, Object> response(BufferedReader reader, ExecutorService executor) throws Exception {
        String line = executor.submit(reader::readLine).get(10, TimeUnit.SECONDS);
        assertNotNull(line, "Server closed stdout unexpectedly");
        return McpJsonDefaults.getMapper().readValue(line, new TypeRef<Map<String, Object>>() {});
    }

    private Map<?, ?> result(Map<String, Object> response) {
        assertFalse(response.containsKey("error"), response.toString());
        return (Map<?, ?>) response.get("result");
    }
}
