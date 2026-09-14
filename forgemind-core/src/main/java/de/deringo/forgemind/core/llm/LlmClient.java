package de.deringo.forgemind.core.llm;

public interface LlmClient {
    LlmResponse chat(LlmRequest request);
}
