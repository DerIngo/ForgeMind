package de.deringo.forgemind.core.tool;

import java.util.Map;

import de.deringo.forgemind.core.permission.Capability;

public interface AgentTool {

    ToolDefinition definition();
    
    Capability capability();

    ToolResult execute(Map<String, Object> arguments);
}