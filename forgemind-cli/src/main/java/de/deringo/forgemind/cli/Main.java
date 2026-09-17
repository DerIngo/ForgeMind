package de.deringo.forgemind.cli;

import java.nio.file.Path;
import java.util.List;

import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.LlmMessage;
import de.deringo.forgemind.core.llm.LlmRequest;
import de.deringo.forgemind.core.llm.LlmResponse;
import de.deringo.forgemind.core.llm.OpenAiCompatibleLlmClient;
import de.deringo.forgemind.core.tool.ToolCall;
import de.deringo.forgemind.core.tool.ToolRegistry;
import de.deringo.forgemind.core.tool.filesystem.ReadFileTool;

public class Main {
    private final static String BASE_URL  = "http://192.168.178.46:1234";
    private final static String API_KEY   = null;
    private final static String LLM_MODEL = "huihui-qwen3-coder-30b-a3b-instruct-abliterated-i1";
    
    private final static String MESSAGE = "Antworte mit genau einem Satz: Was ist Maven?";
    
    public static void main(String[] args) {
        ToolRegistry registry = new ToolRegistry();

        registry.register(
                new ReadFileTool(Path.of("."))
        );
        
        LlmClient llmClient = new OpenAiCompatibleLlmClient(
                BASE_URL,
                API_KEY
        );

        LlmResponse response = llmClient.chat(
                new LlmRequest(
                        LLM_MODEL,
                        List.of(
                                LlmMessage.system("""
                                    You are ForgeMind, a software development agent.
                                    Use the available tools whenever you need
                                    information from the local project.
                                    """),
                                LlmMessage.user(
                                        "Read pom.xml and tell me which Maven modules exist."
                                )
                        ),
                        registry.definitions().stream().toList()
                )
        );

        System.out.println("Content: " + response.content());

        for (ToolCall call : response.toolCalls()) {
            System.out.println("Tool: " + call.name());
            System.out.println("Arguments: " + call.arguments());
        }
    }
}