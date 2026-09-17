package de.deringo.forgemind.core.agent;

import java.time.Duration;

import de.deringo.forgemind.core.tool.ToolCall;
import de.deringo.forgemind.core.tool.ToolResult;

public interface AgentObserver {

    default void onIteration(int iteration) {
    }

    default void onLlmResponse(Duration duration) {
    }
    
    default void onToolCall(ToolCall call) {
    }

    default void onToolResult(
            ToolCall call,
            ToolResult result,
            Duration duration
    ) {
    }

    default void onFinalAnswer(String answer) {
    }
}
