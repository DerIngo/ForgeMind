package de.deringo.forgemind.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;

public final class ForgeMindMCP {
    private ForgeMindMCP() {
    }

    public static void main(String[] args) {
        var transport = new StdioServerTransportProvider(McpJsonDefaults.getMapper());
        var hello = McpSchema.Tool.builder("hello", Map.of(
                "type", "object",
                "properties", Map.of("name", Map.of(
                        "type", "string", "minLength", 1,
                        "description", "Name of the person to greet.")),
                "required", List.of("name"),
                "additionalProperties", false))
                .description("Returns a friendly Hello greeting for the given name.")
                .build();

        var server = McpServer.sync(transport)
                .serverInfo("forgemind-mcp", "0.1.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
                .toolCall(hello, (_, request) -> McpSchema.CallToolResult.builder()
                        .content(List.of(McpSchema.TextContent.builder(
                                "Hello, " + request.arguments().get("name") + "!").build()))
                        .isError(false)
                        .build())
                .build();

        // The SDK owns stdin/stdout and keeps the process alive until stdin closes.
        // Application logs must go to stderr, never to the MCP protocol stream.
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "mcp-shutdown"));
    }
}
