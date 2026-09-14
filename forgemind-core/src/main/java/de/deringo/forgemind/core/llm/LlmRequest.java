package de.deringo.forgemind.core.llm;


public record LlmRequest(
        String model,
        String message
) {
}