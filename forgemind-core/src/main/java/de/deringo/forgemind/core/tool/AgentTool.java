package de.deringo.forgemind.core.tool;

import java.util.Map;

public interface AgentTool {

    ToolDefinition definition();

    ToolResult execute(Map<String, Object> arguments);
}