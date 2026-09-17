package de.deringo.forgemind.core.llm;

import java.util.List;

import de.deringo.forgemind.core.tool.ToolDefinition;

public record LlmRequest(
        String model,
        List<LlmMessage> messages,
        List<ToolDefinition> tools
) {
}