package de.deringo.forgemind.core.agent;

import java.util.ArrayList;
import java.util.List;

import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.LlmMessage;
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

        List<LlmMessage> messages = new ArrayList<>();

        messages.add(LlmMessage.system("""
                You are ForgeMind, a software development agent.
                Be precise and concise.
                """));

        messages.add(LlmMessage.user(input));

        return llmClient
                .chat(new LlmRequest(model, messages, null))
                .content();
    }
}