package de.deringo.forgemind.core.agent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.LlmMessage;
import de.deringo.forgemind.core.llm.LlmRequest;
import de.deringo.forgemind.core.llm.LlmResponse;
import de.deringo.forgemind.core.permission.PermissionDecision;
import de.deringo.forgemind.core.permission.PermissionHandler;
import de.deringo.forgemind.core.permission.PermissionPolicy;
import de.deringo.forgemind.core.permission.PermissionRequest;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolCall;
import de.deringo.forgemind.core.tool.ToolRegistry;
import de.deringo.forgemind.core.tool.ToolResult;
import de.deringo.forgemind.core.tool.ToolResultTruncator;

public final class AgentLoop implements Agent {

    private static final int MAX_ITERATIONS = 20;

    private final LlmClient llmClient;
    private final String model;
    private final ToolRegistry toolRegistry;
    private final AgentObserver observer;
    
    private final PermissionPolicy permissionPolicy;
    private final PermissionHandler permissionHandler;

    public AgentLoop(
            LlmClient llmClient,
            String model,
            ToolRegistry toolRegistry,
            AgentObserver observer,
            PermissionPolicy permissionPolicy,
            PermissionHandler permissionHandler
    ) {
        this.llmClient = llmClient;
        this.model = model;
        this.toolRegistry = toolRegistry;
        this.observer = observer;
        this.permissionPolicy = permissionPolicy;
        this.permissionHandler = permissionHandler;
    }

    @Override
    public String run(String input) {

        String systemPrompt = getSystemPrompt();

        List<LlmMessage> messages = new ArrayList<>();

        messages.add(LlmMessage.system(systemPrompt));

        messages.add(LlmMessage.user(input));

        for (int iteration = 0;
             iteration < MAX_ITERATIONS;
             iteration++) {

            observer.onIteration(iteration);
            
            long llmStart = System.nanoTime();

            LlmResponse response = llmClient.chat(
                    new LlmRequest(
                            model,
                            messages,
                            toolRegistry.definitions()
                                    .stream()
                                    .toList()
                    )
            );

            Duration llmDuration = Duration.ofNanos(
                    System.nanoTime() - llmStart
            );

            observer.onLlmResponse(llmDuration);

            /*
             * No tool call:
             * The model considers the task finished.
             */
            if (!response.hasToolCalls()) {
                observer.onFinalAnswer(response.content());
                return response.content();
            }

            /*
             * Important:
             * Add the assistant's tool-call message
             * to the conversation first.
             */
            messages.add(
                    LlmMessage.assistant(
                            response.content(),
                            response.toolCalls()
                    )
            );

            /*
             * Execute requested tools.
             */
            for (ToolCall call : response.toolCalls()) {

                observer.onToolCall(call);
                
                AgentTool tool = toolRegistry.get(call.name());

                PermissionRequest permissionRequest =
                        new PermissionRequest(
                                tool.capability(),
                                call.name(),
                                call.arguments()
                        );

                PermissionDecision decision =
                        permissionPolicy.evaluate(permissionRequest);

                boolean allowed = switch (decision) {

                    case ALLOW -> true;

                    case DENY -> false;

                    case ASK ->
                            permissionHandler.requestPermission(
                                    permissionRequest
                            );
                };

                ToolResult result;

                if (!allowed) {
                    result = new ToolResult(
                            "Permission denied by user."
                    );
                } else {

                    long toolStart = System.nanoTime();

                    result = executeTool(tool, call);
                    result = ToolResultTruncator.truncate(result);

                    Duration toolDuration = Duration.ofNanos(
                            System.nanoTime() - toolStart
                    );

                    observer.onToolResult(
                            call,
                            result,
                            toolDuration
                    );
                }
                
                /*
                 * Return tool result to the LLM.
                 */
                messages.add(
                        LlmMessage.tool(
                                call.id(),
                                result.content()
                        )
                );
            }
        }

        throw new IllegalStateException(
                "Agent exceeded maximum number of iterations: "
                        + MAX_ITERATIONS
        );
    }
    
    private ToolResult executeTool(
            AgentTool tool,
            ToolCall call
    ) {
        try {
            return tool.execute(call.arguments());
        } catch (Exception e) {
            return new ToolResult(
                    "ERROR: "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage()
            );
        }
    }
    
    private String getSystemPrompt() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String javaVersion = System.getProperty("java.version");
        
        String systemPrompt = """
                You are ForgeMind, a software development agent.

                Environment:
                - Operating system: %s
                - OS version: %s
                - Java: %s

                Use the available tools whenever information from the local project
                is required. Base your answer on inspected source code and never guess
                file contents or project structure.

                Tool selection rules:

                1. When looking for an implementation, class, interface, method, or
                   component, your first tool call MUST be search_files using the most
                   specific name or keyword available from the user's request.

                2. Do not use list_files to navigate source directories one level at
                   a time.

                3. Use list_files only when the user explicitly requests a directory
                   overview or search_files did not find a relevant path.

                4. Read only the primary implementation and direct dependencies that
                   are necessary to answer the question.

                5. Do not read placeholder, generated, build-output, example, or
                   unrelated files.

                6. Request independent tool calls together when possible.

                7. Stop calling tools as soon as the inspected source code is
                   sufficient to answer the user's question.
                   
                8. Do not repeat a tool call with identical arguments
                   unless the previous execution failed or there is a clear reason to repeat it.

                Clearly distinguish verified facts from assumptions.
                """.formatted(
                        osName,
                        osVersion,
                        javaVersion
                );
       
        return systemPrompt;
    }
}