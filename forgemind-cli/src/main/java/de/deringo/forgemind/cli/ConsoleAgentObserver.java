package de.deringo.forgemind.cli;

import java.time.Duration;

import de.deringo.forgemind.core.agent.AgentObserver;
import de.deringo.forgemind.core.tool.ToolCall;
import de.deringo.forgemind.core.tool.ToolResult;

public final class ConsoleAgentObserver implements AgentObserver {

    @Override
    public void onIteration(int iteration) {
        System.out.println();
        System.out.println("=== Agent iteration " + iteration + " ===");
    }

    @Override
    public void onToolCall(ToolCall call) {
        System.out.println("Tool call: " + call.name());
        System.out.println("Arguments: " + call.arguments());
    }

    @Override
    public void onLlmResponse(Duration duration) {
        System.out.printf(
                "LLM response: %.2f s%n",
                duration.toMillis() / 1000.0
        );
    }

    @Override
    public void onToolResult(
            ToolCall call,
            ToolResult result,
            Duration duration
    ) {
        System.out.printf(
                "Tool result: %s (%d chars, %.2f ms)%n",
                call.name(),
                result.content().length(),
                duration.toNanos() / 1_000_000.0
        );

        String firstLine = result.content().lines().findFirst().orElse("");
        boolean commandResult = call.name().equals("run_command")
                && firstLine.startsWith("Exit code: ");
        boolean failedCommand = commandResult && !firstLine.equals("Exit code: 0");

        if (failedCommand || result.content().startsWith("ERROR:")
                || result.content().startsWith("Permission denied")
                || result.content().length() <= 500) {

            System.out.println();
            System.out.println(result.content());
        } else if (commandResult) {
            System.out.println(firstLine);
        }
    }

    @Override
    public void onFinalAnswer(String answer) {
        System.out.println();
        System.out.println("=== Final answer ===");
    }
}
