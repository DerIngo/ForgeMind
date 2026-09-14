package de.deringo.forgemind.cli;

import de.deringo.forgemind.core.agent.Agent;
import de.deringo.forgemind.core.agent.AgentLoop;
import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.OpenAiCompatibleLlmClient;

public class Main {
    private final static String BASE_URL  = "http://192.168.178.46:1234";
    private final static String API_KEY   = null;
    private final static String LLM_MODEL = "qwen3-coder";
    
    private final static String MESSAGE = "Antworte mit genau einem Satz: Was ist Maven?";
    
    public static void main(String[] args) {

        LlmClient llmClient = new OpenAiCompatibleLlmClient(
                BASE_URL,
                API_KEY
        );

        Agent agent = new AgentLoop(
                llmClient,
                LLM_MODEL
        );

        String result = agent.run(
                MESSAGE
        );

        System.out.println(result);
    }
}