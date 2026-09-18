package de.deringo.forgemind.cli;

import java.nio.file.Path;
import java.time.Duration;

import de.deringo.forgemind.core.agent.Agent;
import de.deringo.forgemind.core.agent.AgentLoop;
import de.deringo.forgemind.core.agent.DefaultSystemPromptProvider;
import de.deringo.forgemind.core.agent.SystemPromptProvider;
import de.deringo.forgemind.core.command.CommandExecutor;
import de.deringo.forgemind.core.git.GitExecutor;
import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.OpenAiCompatibleLlmClient;
import de.deringo.forgemind.core.permission.DefaultPermissionPolicy;
import de.deringo.forgemind.core.permission.InMemoryPermissionStore;
import de.deringo.forgemind.core.permission.PermissionHandler;
import de.deringo.forgemind.core.permission.PermissionPolicy;
import de.deringo.forgemind.core.permission.PermissionStore;
import de.deringo.forgemind.core.process.ProcessExecutor;
import de.deringo.forgemind.core.tool.ToolRegistry;
import de.deringo.forgemind.core.tool.command.RunCommandTool;
import de.deringo.forgemind.core.tool.filesystem.ListFilesTool;
import de.deringo.forgemind.core.tool.filesystem.ReadFileTool;
import de.deringo.forgemind.core.tool.filesystem.ReplaceTextTool;
import de.deringo.forgemind.core.tool.filesystem.SearchFilesTool;
import de.deringo.forgemind.core.tool.filesystem.SearchTextTool;
import de.deringo.forgemind.core.tool.git.GitDiffTool;
import de.deringo.forgemind.core.tool.git.GitStatusTool;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

public class Main {
    private final static String BASE_URL  = "http://192.168.178.46:1234";
    private final static String API_KEY   = null;
    private final static String LLM_MODEL = "huihui-qwen3-coder-30b-a3b-instruct-abliterated-i1";
    
    
    public static void main(String[] args) {
        long start = System.nanoTime();
        
        Path projectRoot = Path.of("..")
                .toAbsolutePath()
                .normalize();
        System.out.println("Project root: " + projectRoot);

        ProjectWorkspace workspace =
                new ProjectWorkspace(projectRoot);

        ProcessExecutor processExecutor =
                new ProcessExecutor();

        CommandExecutor commandExecutor =
                new CommandExecutor(
                        workspace,
                        processExecutor
                );

        GitExecutor gitExecutor =
                new GitExecutor(
                        workspace,
                        processExecutor
                );
        
        LlmClient llmClient =
                new OpenAiCompatibleLlmClient(
                        BASE_URL,
                        API_KEY
                );

        SystemPromptProvider systemPromptProvider = new DefaultSystemPromptProvider();

        ToolRegistry tools = new ToolRegistry();

        tools.register(
                new ReadFileTool(workspace)
        );
        
        tools.register(
                new ListFilesTool(workspace)
        );

        tools.register(
                new SearchFilesTool(workspace)
        );
        
        tools.register(
                new SearchTextTool(workspace)
        );
        
        tools.register(new ReplaceTextTool(workspace));
        
        tools.register(
                new RunCommandTool(commandExecutor)
        );
        
        tools.register(
                new GitStatusTool(
                        gitExecutor
                )
        );

        tools.register(
                new GitDiffTool(
                        gitExecutor,
                        workspace
                )
        );
        
        PermissionPolicy permissionPolicy =
                new DefaultPermissionPolicy();

        PermissionHandler permissionHandler =
                new ConsolePermissionHandler();

        PermissionStore permissionStore =
                new InMemoryPermissionStore();
        
        Agent agent = new AgentLoop(
                llmClient,
                LLM_MODEL,
                systemPromptProvider,
                tools,
                new ConsoleAgentObserver(),
                permissionPolicy,
                permissionHandler,
                permissionStore
        );

        String result = agent.run("""
                Inspect the current Git working tree and summarize what has changed.
                Do not modify any files.
                """);

        System.out.println(result);
        
        Duration duration = Duration.ofNanos(System.nanoTime() - start);
        System.out.printf("%n%nDuration: %.2f s%n", duration.toMillis() / 1000.0);
    }
}