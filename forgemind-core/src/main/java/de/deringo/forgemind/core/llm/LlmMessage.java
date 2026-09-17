package de.deringo.forgemind.core.llm;


import java.util.List;

import de.deringo.forgemind.core.tool.ToolCall;

public record LlmMessage(
        Role role,
        String content,
        List<ToolCall> toolCalls,
        String toolCallId
) {

    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    public static LlmMessage system(String content) {
        return new LlmMessage(Role.SYSTEM, content, List.of(), null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(Role.USER, content, List.of(), null);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(Role.ASSISTANT, content, List.of(), null);
    }

    public static LlmMessage assistant(
            String content,
            List<ToolCall> toolCalls
    ) {
        return new LlmMessage(
                Role.ASSISTANT,
                content,
                toolCalls,
                null
        );
    }

    public static LlmMessage tool(
            String toolCallId,
            String content
    ) {
        return new LlmMessage(
                Role.TOOL,
                content,
                List.of(),
                toolCallId
        );
    }
}