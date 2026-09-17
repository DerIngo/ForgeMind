package de.deringo.forgemind.core.tool;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ToolRegistry {

    private final Map<String, AgentTool> tools = new HashMap<>();

    public void register(AgentTool tool) {
        tools.put(tool.definition().name(), tool);
    }

    public AgentTool get(String name) {

        AgentTool tool = tools.get(name);

        if (tool == null) {
            throw new IllegalArgumentException(
                    "Unknown tool: " + name
            );
        }

        return tool;
    }

    public Collection<ToolDefinition> definitions() {
        return tools.values()
                .stream()
                .map(AgentTool::definition)
                .toList();
    }
}