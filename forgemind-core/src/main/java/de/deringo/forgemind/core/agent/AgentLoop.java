package de.deringo.forgemind.core.agent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.LlmMessage;
import de.deringo.forgemind.core.llm.LlmRequest;
import de.deringo.forgemind.core.llm.LlmResponse;
import de.deringo.forgemind.core.permission.PermissionDecision;
import de.deringo.forgemind.core.permission.PermissionGrant;
import de.deringo.forgemind.core.permission.PermissionHandler;
import de.deringo.forgemind.core.permission.PermissionKey;
import de.deringo.forgemind.core.permission.PermissionPolicy;
import de.deringo.forgemind.core.permission.PermissionRequest;
import de.deringo.forgemind.core.permission.PermissionStore;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolCall;
import de.deringo.forgemind.core.tool.ToolRegistry;
import de.deringo.forgemind.core.tool.ToolResult;
import de.deringo.forgemind.core.tool.ToolResultTruncator;

public final class AgentLoop implements Agent {

    private static final int MAX_ITERATIONS = 20;
    private final SystemPromptProvider systemPromptProvider;
    
    private final LlmClient llmClient;
    private final String model;
    private final ToolRegistry toolRegistry;
    private final AgentObserver observer;
    
    private final PermissionPolicy permissionPolicy;
    private final PermissionHandler permissionHandler;
    private final PermissionStore permissionStore;

    public AgentLoop(
            LlmClient llmClient,
            String model,
            SystemPromptProvider systemPromptProvider,
            ToolRegistry toolRegistry,
            AgentObserver observer,
            PermissionPolicy permissionPolicy,
            PermissionHandler permissionHandler,
            PermissionStore permissionStore
    ) {
        this.llmClient = llmClient;
        this.model = model;
        this.systemPromptProvider = systemPromptProvider;
        this.toolRegistry = toolRegistry;
        this.observer = observer;
        this.permissionPolicy = permissionPolicy;
        this.permissionHandler = permissionHandler;
        this.permissionStore = permissionStore;
    }

    @Override
    public String run(String input) {

        List<LlmMessage> messages = new ArrayList<>();

        messages.add(LlmMessage.system(systemPromptProvider.createSystemPrompt()));

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

                PermissionKey permissionKey =
                        tool.permissionKey(
                                call.arguments()
                        );

                PermissionRequest permissionRequest =
                        new PermissionRequest(
                                tool.capability(),
                                call.name(),
                                call.arguments(),
                                permissionKey
                        );

                PermissionDecision decision =
                        permissionPolicy.evaluate(
                                permissionRequest
                        );

                ToolResult result;

                boolean allowed;

                switch (decision) {

                    case ALLOW ->
                            allowed = true;

                    case DENY ->
                            allowed = false;

                    case ASK -> {

                        if (permissionStore.isAllowed(
                                permissionKey
                        )) {
                            allowed = true;

                        } else {

                            PermissionGrant grant =
                                    permissionHandler
                                            .requestPermission(
                                                    permissionRequest
                                            );

                            allowed = grant.allowed();

                            if (allowed) {
                                permissionStore.allow(
                                        permissionKey,
                                        grant.scope()
                                );
                            }
                        }
                    }

                    default ->
                            throw new IllegalStateException(
                                    "Unexpected permission decision: "
                                            + decision
                            );
                }
                
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
}   
 