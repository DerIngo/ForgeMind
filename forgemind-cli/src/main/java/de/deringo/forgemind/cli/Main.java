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
Add a small utility class named TextUtils to forgemind-core.

Requirements:
- Package: de.deringo.forgemind.core.util
- TextUtils must not be instantiable.
- Add a public static method isBlank(String value).
- The method returns true when value is null, empty, or contains only whitespace.
- Add JUnit tests covering null, empty string, whitespace-only text, and non-blank text.
- Do not modify unrelated files.
- Run the appropriate Maven tests after implementation.
- If verification succeeds, stop and summarize the changes.
                """);

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
