package de.deringo.forgemind.core.tool;

import java.util.Map;

public record ToolCall(
        String id,
        String name,
        Map<String, Object> arguments
) {
}