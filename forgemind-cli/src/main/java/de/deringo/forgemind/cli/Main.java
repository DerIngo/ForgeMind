package de.deringo.forgemind.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import de.deringo.forgemind.core.agent.Agent;
import de.deringo.forgemind.core.agent.AgentLoop;
import de.deringo.forgemind.core.agent.DefaultSystemPromptProvider;
import de.deringo.forgemind.core.agent.SystemPromptProvider;
import de.deringo.forgemind.core.command.CommandExecutor;
import de.deringo.forgemind.core.git.GitExecutor;
import de.deringo.forgemind.core.mcp.McpToolProvider;
import de.deringo.forgemind.core.patch.PatchApplier;
import de.deringo.forgemind.core.patch.PatchParser;
import de.deringo.forgemind.core.tool.filesystem.ApplyPatchTool;
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
import de.deringo.forgemind.core.tool.filesystem.WriteFileTool;
import de.deringo.forgemind.core.tool.git.GitDiffTool;
import de.deringo.forgemind.core.tool.git.GitStatusTool;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;

public class Main {
    
    private final static String BASE_URL  = "http://192.168.178.46:1234";
    private final static String API_KEY   = null;
    private final static String LLM_MODEL = "huihui-qwen3-coder-30b-a3b-instruct-abliterated-i1";

    /*
    private final static String BASE_URL  = "https://api.groq.com/openai";
    private final static String API_KEY   = "secret";
    private final static String LLM_MODEL = "openai/gpt-oss-120b";
    */
    
    public static void main(String[] args) {
        long start = System.nanoTime();
        
        ProjectWorkspace workspace =
                new ProjectWorkspace(getProjectRoot());

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
        tools.register(new ApplyPatchTool(new PatchParser(), new PatchApplier(workspace)));
        
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
        
        tools.register(
                new WriteFileTool(workspace)
        );

        String mcpEndpoint = System.getenv("FORGEMIND_MCP_URL");
        mcpEndpoint = mcpEndpoint != null ? mcpEndpoint : "http://127.0.0.1:8080/mcp";
        if (mcpEndpoint != null && !mcpEndpoint.isBlank()) {
            McpToolProvider mcpTools = McpToolProvider.connect("demo", mcpEndpoint);
            try {
                mcpTools.registerTools(tools);
            } catch (RuntimeException e) {
                mcpTools.close();
                throw e;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(mcpTools::close, "mcp-client-shutdown"));
            System.out.println("Connected to MCP server: " + mcpEndpoint);
        }
        
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

        String task = args.length > 0
                ? String.join(" ", args)
                : """
Begrüße Ingo mit dem hello-Tool.
                """;
        String result = agent.run(task);

        System.out.println(result);
        
        Duration duration = Duration.ofNanos(System.nanoTime() - start);
        System.out.printf("%n%nDuration: %.2f s%n", duration.toMillis() / 1000.0);
    }
    
    private static Path getProjectRoot() {
        Path[] candidates = {
                Path.of("."),
                Path.of("..")
        };

        for (Path candidate : candidates) {
            Path projectRoot = candidate
                    .toAbsolutePath()
                    .normalize();

            boolean isForgeMindRoot =
                    Files.isRegularFile(
                            projectRoot.resolve("pom.xml")
                    )
                    && Files.isDirectory(
                            projectRoot.resolve("forgemind-core")
                    )
                    && Files.isDirectory(
                            projectRoot.resolve("forgemind-cli")
                    );

            if (isForgeMindRoot) {
                System.out.println(
                        "Project root: " + projectRoot
                );
                return projectRoot;
            }
        }

        throw new IllegalStateException(
                "Could not find the ForgeMind project root. "
                + "Start the application from the ForgeMind root "
                + "or its forgemind-cli directory."
        );
    }
}
