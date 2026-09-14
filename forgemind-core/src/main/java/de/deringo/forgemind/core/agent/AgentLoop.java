package de.deringo.forgemind.core.agent;

import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.LlmRequest;

public final class AgentLoop implements Agent {

    private final LlmClient llmClient;
    private final String model;

    public AgentLoop(
            LlmClient llmClient,
            String model
    ) {
        this.llmClient = llmClient;
        this.model = model;
    }

    @Override
    public String run(String input) {
        return llmClient
                .chat(new LlmRequest(model, input))
                .content();
    }
}