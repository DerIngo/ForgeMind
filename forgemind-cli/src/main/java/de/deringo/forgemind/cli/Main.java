package de.deringo.forgemind.cli;

import java.nio.file.Path;

import de.deringo.forgemind.core.agent.Agent;
import de.deringo.forgemind.core.agent.AgentLoop;
import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.OpenAiCompatibleLlmClient;
import de.deringo.forgemind.core.tool.ToolRegistry;
import de.deringo.forgemind.core.tool.filesystem.ListFilesTool;
import de.deringo.forgemind.core.tool.filesystem.ReadFileTool;
import de.deringo.forgemind.core.tool.filesystem.SearchFilesTool;

public class Main {
    private final static String BASE_URL  = "http://192.168.178.46:1234";
    private final static String API_KEY   = null;
    private final static String LLM_MODEL = "huihui-qwen3-coder-30b-a3b-instruct-abliterated-i1";
    
    private final static String MESSAGE = "Antworte mit genau einem Satz: Was ist Maven?";
    
    public static void main(String[] args) {

        Path projectRoot = Path.of("..")
                .toAbsolutePath()
                .normalize();
        System.out.println("Project root: " + projectRoot);

        LlmClient llmClient =
                new OpenAiCompatibleLlmClient(
                        BASE_URL,
                        API_KEY
                );

        ToolRegistry tools = new ToolRegistry();

        tools.register(
                new ReadFileTool(projectRoot)
        );
        
        tools.register(
                new ListFilesTool(projectRoot)
        );

        tools.register(
                new SearchFilesTool(projectRoot)
        );
        
        Agent agent = new AgentLoop(
                llmClient,
                LLM_MODEL,
                tools
        );

        String result = agent.run("""
                Find the implementation of the ForgeMind agent loop.

                Analyze it and explain briefly how tool calls are executed.
                Do not ask me for file paths. Explore the project yourself.
                """);

        System.out.println(result);
    }
}