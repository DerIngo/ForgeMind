package de.deringo.forgemind.core.llm;

import java.util.List;

import de.deringo.forgemind.core.tool.ToolCall;

public record LlmResponse(
        String content,
        List<ToolCall> toolCalls
) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}