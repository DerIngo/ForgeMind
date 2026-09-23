package de.deringo.forgemind.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/** Runs the hello example as a client, with its own server subprocess. */
public final class McpDemoClient {
    private McpDemoClient() {
    }

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && args[0].isBlank())) {
            throw new IllegalArgumentException("Usage: McpDemoClient [name]");
        }
        String name = args.length == 0 ? "World" : args[0];
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        // Works both from the IDE and from the shaded JAR's classpath.
        var parameters = ServerParameters.builder(java)
                .args("-cp", System.getProperty("java.class.path"), ForgeMindMCP.class.getName())
                .build();
        var transport = new StdioClientTransport(parameters, McpJsonDefaults.getMapper());
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
            // Closes the stdio connection and releases the server subprocess.
            client.closeGracefully();
        }
        System.out.println("Connection closed.");
    }
}
