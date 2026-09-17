package de.deringo.forgemind.core.tool;

import java.util.Map;

public record ToolDefinition(
        String name,
        String description,
        Map<String, Object> parameters
) {
}