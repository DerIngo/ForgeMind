package de.deringo.forgemind.cli;

import java.nio.file.Path;
import java.time.Duration;

import de.deringo.forgemind.core.agent.Agent;
import de.deringo.forgemind.core.agent.AgentLoop;
import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.OpenAiCompatibleLlmClient;
import de.deringo.forgemind.core.tool.ToolRegistry;
import de.deringo.forgemind.core.tool.filesystem.ListFilesTool;
import de.deringo.forgemind.core.tool.filesystem.ReadFileTool;
import de.deringo.forgemind.core.tool.filesystem.SearchFilesTool;
import de.deringo.forgemind.core.tool.filesystem.SearchTextTool;

public class Main {
    private final static String BASE_URL  = "http://192.168.178.46:1234";
    private final static String API_KEY   = null;
    private final static String LLM_MODEL = "huihui-qwen3-coder-30b-a3b-instruct-abliterated-i1";
    
    private final static String MESSAGE = "Antworte mit genau einem Satz: Was ist Maven?";
    
    public static void main(String[] args) {
        long start = System.nanoTime();
        
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
        
        tools.register(
                new SearchTextTool(projectRoot)
        );
        
        Agent agent = new AgentLoop(
                llmClient,
                LLM_MODEL,
                tools,
                new ConsoleAgentObserver()
        );

        String result = agent.run("""
                Analyze how tools are registered and executed in this project.

                Find the relevant implementation yourself.
                Explain the complete flow from tool registration
                until the tool result is returned to the LLM.
                """);

        System.out.println(result);
        
        Duration duration = Duration.ofNanos(System.nanoTime() - start);
        System.out.printf("%n%nDuration: %.2f s%n", duration.toMillis() / 1000.0);
    }
}