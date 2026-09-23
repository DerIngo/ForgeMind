package de.deringo.forgemind.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

/** Connects to an independently running HTTP MCP server. */
public final class McpDemoClient {
    private McpDemoClient() {
    }

    public static void main(String[] args) {
        if (args.length > 2 || (args.length > 1 && args[1].isBlank())) {
            throw new IllegalArgumentException("Usage: McpDemoClient [base-url] [name]");
        }
        String baseUrl = args.length == 0 ? "http://127.0.0.1:8080" : args[0];
        URI uri = URI.create(baseUrl);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getQuery() != null || uri.getFragment() != null
                || uri.getUserInfo() != null || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))) {
            throw new IllegalArgumentException("Expected an HTTP(S) base URL without path; endpoint is /mcp.");
        }
        String name = args.length > 1 ? args[1] : "World";
        var transport = HttpClientStreamableHttpTransport.builder(baseUrl)
                .endpoint("/mcp").build();
        var client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();
        try {
            var initialized = client.initialize();
            System.out.println("Connected to: " + initialized.serverInfo().name());
            System.out.println("Protocol: " + initialized.protocolVersion());
            System.out.println("Available tools:");
            for (var tool : client.listTools().tools()) {
                System.out.println("- " + tool.name() + ": " + tool.description());
            }
            System.out.println("Calling hello with name: " + name);
            var result = client.callTool(McpSchema.CallToolRequest.builder("hello")
                    .arguments(Map.of("name", name))
                    .build());
            if (Boolean.TRUE.equals(result.isError())) {
                throw new IllegalStateException("hello failed: " + result.content());
            }
            for (var content : result.content()) {
                if (content instanceof McpSchema.TextContent text) {
                    System.out.println(text.text());
                }
            }
        } finally {
            // Closes this client session; the independent server keeps running.
            client.closeGracefully();
        }
        System.out.println("Connection closed.");
    }
}
