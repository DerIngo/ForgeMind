package de.deringo.forgemind.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.McpSyncServer;
import org.apache.catalina.startup.Tomcat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;

public final class ForgeMindMCP implements AutoCloseable {
    private final Tomcat http = new Tomcat();
    private McpSyncServer server;
    private Path baseDirectory;
    private ForgeMindMCP() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 2) {
            throw new IllegalArgumentException("Usage: ForgeMindMCP [port] [bind-address]");
        }
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String host = args.length > 1 ? args[1] : "127.0.0.1";
        var application = start(host, port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                application.close();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }, "mcp-shutdown"));
        System.out.println("MCP server listening on http://" + host + ":" + application.port() + "/mcp");
        application.http.getServer().await();
    }

    static ForgeMindMCP start(String host, int port) throws Exception {
        if (port < 0 || port > 65535 || host.isBlank()) {
            throw new IllegalArgumentException("Valid bind-address and port (0-65535) required.");
        }
        var application = new ForgeMindMCP();
        try {
            application.startHttp(host, port);
            return application;
        } catch (Exception e) {
            try { application.close(); } catch (Exception cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }

    private void startHttp(String host, int port) throws Exception {
        var transport = HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(McpJsonDefaults.getMapper()).mcpEndpoint("/mcp").build();
        var hello = McpSchema.Tool.builder("hello", Map.of(
                "type", "object",
                "properties", Map.of("name", Map.of(
                        "type", "string", "minLength", 1,
                        "description", "Name of the person to greet.")),
                "required", List.of("name"),
                "additionalProperties", false))
                .description("Returns a friendly Hello greeting for the given name.")
                .build();

        server = McpServer.sync(transport)
                .serverInfo("forgemind-mcp", "0.1.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
                .toolCall(hello, (_, request) -> McpSchema.CallToolResult.builder()
                        .content(List.of(McpSchema.TextContent.builder(
                                "Hello, " + request.arguments().get("name") + "!").build()))
                        .isError(false)
                        .build())
                .build();

        baseDirectory = Files.createTempDirectory("forgemind-mcp-");
        http.setBaseDir(baseDirectory.toString());
        http.setPort(port);
        http.getConnector().setProperty("address", host);
        var context = http.addContext("", baseDirectory.toString());
        var servlet = Tomcat.addServlet(context, "mcp", transport);
        servlet.setAsyncSupported(true);
        context.addServletMappingDecoded("/mcp", "mcp");
        http.start();
        if (!http.getConnector().getState().isAvailable()) {
            throw new IllegalStateException("HTTP connector could not start on " + host + ":" + port);
        }
    }

    int port() {
        return http.getConnector().getLocalPort();
    }

    @Override
    public void close() throws Exception {
        try {
            if (server != null) {
                server.closeGracefully();
            }
        } finally {
            try {
                http.stop();
            } finally {
                http.destroy();
                if (baseDirectory != null) {
                    try (var paths = Files.walk(baseDirectory)) {
                        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                            Files.deleteIfExists(path);
                        }
                    }
                }
            }
        }
    }
}