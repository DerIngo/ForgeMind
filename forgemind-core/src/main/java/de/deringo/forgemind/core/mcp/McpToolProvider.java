package de.deringo.forgemind.core.mcp;

import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.permission.PermissionKey;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolRegistry;
import de.deringo.forgemind.core.tool.ToolResult;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Connects to one MCP server and exposes its tools to ForgeMind. */
public final class McpToolProvider implements AutoCloseable {
    private final String serverId;
    private final String serverUrl;
    private final McpSyncClient client;

    private McpToolProvider(String serverId, String serverUrl, McpSyncClient client) {
        this.serverId = serverId;
        this.serverUrl = serverUrl;
        this.client = client;
    }

    public static McpToolProvider connect(String serverId, String endpoint) {
        if (serverId == null || !serverId.matches("[A-Za-z][A-Za-z0-9_-]*")) {
            throw new IllegalArgumentException("MCP server id must start with a letter and contain only letters, digits, '_' or '-'.");
        }
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid MCP server URL: " + endpoint, e);
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("MCP endpoint must be an absolute HTTP(S) URL without credentials, query, or fragment.");
        }

        String baseUrl = uri.getScheme() + "://" + uri.getRawAuthority();
        String mcpPath = uri.getRawPath() == null || uri.getRawPath().isBlank() || uri.getRawPath().equals("/")
                ? "/mcp" : uri.getRawPath();
        var transport = HttpClientStreamableHttpTransport.builder(baseUrl)
                .endpoint(mcpPath)
                .build();
        var client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();
        try {
            client.initialize();
            return new McpToolProvider(serverId, endpoint, client);
        } catch (RuntimeException e) {
            client.closeGracefully();
            throw new IllegalStateException("Could not initialize MCP server '" + serverId + "' at " + endpoint, e);
        }
    }

    /** Discovers the server's tools and registers them under mcp_<serverId>_<toolName>. */
    public void registerTools(ToolRegistry registry) {
        List<McpSchema.Tool> remoteTools = client.listTools().tools();
        List<String> names = remoteTools.stream().map(tool -> exposedName(tool.name())).toList();
        if (names.stream().distinct().count() != names.size()) {
            throw new IllegalArgumentException("MCP server exposes tool names that collide after ForgeMind prefixing.");
        }
        for (String name : names) {
            if (registry.definitions().stream().anyMatch(definition -> definition.name().equals(name))) {
                throw new IllegalArgumentException("MCP tool name already registered: " + name);
            }
        }
        for (McpSchema.Tool remoteTool : remoteTools) {
            registry.register(new RemoteTool(remoteTool, exposedName(remoteTool.name())));
        }
    }

    private String exposedName(String remoteName) {
        if (remoteName == null || !remoteName.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("MCP server exposes an invalid tool name: " + remoteName);
        }
        return "mcp_" + serverId.replace('-', '_') + "_" + remoteName.replace('-', '_');
    }

    @Override
    public void close() {
        client.closeGracefully();
    }

    private final class RemoteTool implements AgentTool {
        private final String remoteName;
        private final ToolDefinition definition;

        private RemoteTool(McpSchema.Tool tool, String exposedName) {
            this.remoteName = tool.name();
            Map<String, Object> schema = tool.inputSchema() == null
                    ? Map.of("type", "object", "properties", Map.of())
                    : Map.copyOf(tool.inputSchema());
            this.definition = new ToolDefinition(exposedName,
                    tool.description() == null ? "MCP tool from " + serverId : tool.description(), schema);
        }

        @Override
        public ToolDefinition definition() {
            return definition;
        }

        @Override
        public Capability capability() {
            return Capability.NETWORK;
        }

        @Override
        public PermissionKey permissionKey(Map<String, Object> arguments) {
            return new PermissionKey(Capability.NETWORK, definition.name(), serverUrl + "#" + remoteName);
        }

        @Override
        public ToolResult execute(Map<String, Object> arguments) {
            var result = client.callTool(McpSchema.CallToolRequest.builder(remoteName)
                    .arguments(arguments == null ? Map.of() : new LinkedHashMap<>(arguments))
                    .build());
            String content = result.content().stream()
                    .filter(McpSchema.TextContent.class::isInstance)
                    .map(McpSchema.TextContent.class::cast)
                    .map(McpSchema.TextContent::text)
                    .reduce((left, right) -> left + System.lineSeparator() + right)
                    .orElse("MCP tool returned no text content.");
            if (Boolean.TRUE.equals(result.isError())) {
                throw new IllegalStateException("MCP tool '" + remoteName + "' failed: " + content);
            }
            return new ToolResult(content);
        }
    }
}
