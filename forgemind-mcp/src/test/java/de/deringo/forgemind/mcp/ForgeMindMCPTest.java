package de.deringo.forgemind.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ForgeMindMCPTest {
    @Test
    void servesIndependentHttpSessionsAndValidatesArguments() throws Exception {
        try (var server = ForgeMindMCP.start("127.0.0.1", 0)) {
            // Closing a client must not terminate the server or prevent the next client.
            for (int session = 0; session < 2; session++) {
                var transport = HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + server.port())
                        .endpoint("/mcp").build();
                var client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
                try {
                    assertEquals("forgemind-mcp", client.initialize().serverInfo().name());
                    var tools = client.listTools().tools();
                    assertEquals(1, tools.size());
                    assertEquals("hello", tools.getFirst().name());
                    var greeting = client.callTool(McpSchema.CallToolRequest.builder("hello")
                            .arguments(Map.of("name", "Welt")).build());
                    assertFalse(Boolean.TRUE.equals(greeting.isError()));
                    assertEquals("Hello, Welt!", ((McpSchema.TextContent) greeting.content().getFirst()).text());
                    for (var arguments : List.of(Map.<String, Object>of(), Map.<String, Object>of("name", 42),
                            Map.<String, Object>of("name", ""))) {
                        assertTrue(client.callTool(McpSchema.CallToolRequest.builder("hello")
                                .arguments(arguments).build()).isError());
                    }
                } finally {
                    client.closeGracefully();
                }
            }
        }
    }
}
