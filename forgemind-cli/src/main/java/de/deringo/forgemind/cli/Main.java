package de.deringo.forgemind.cli;

import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.LlmRequest;
import de.deringo.forgemind.core.llm.LlmResponse;
import de.deringo.forgemind.core.llm.OpenAiCompatibleLlmClient;

public class Main {

    public static void main(String[] args) {

        LlmClient client = new OpenAiCompatibleLlmClient(
                "http://192.168.178.46:1234",
                null
        );

        LlmResponse response = client.chat(
                new LlmRequest(
                        "qwen3-coder",
                        "Antworte mit genau einem Satz: Was ist Maven?"
                )
        );

        System.out.println(response.content());
    }
}