package de.deringo.forgemind.core.agent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.LlmMessage;
import de.deringo.forgemind.core.llm.LlmRequest;
import de.deringo.forgemind.core.llm.LlmResponse;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolCall;
import de.deringo.forgemind.core.tool.ToolRegistry;
import de.deringo.forgemind.core.tool.ToolResult;

public final class AgentLoop implements Agent {

    private static final int MAX_ITERATIONS = 20;

    private final LlmClient llmClient;
    private final String model;
    private final ToolRegistry toolRegistry;
    private final AgentObserver observer;
    
    public AgentLoop(
            LlmClient llmClient,
            String model,
            ToolRegistry toolRegistry,
            AgentObserver observer
    ) {
        this.llmClient = llmClient;
        this.model = model;
        this.toolRegistry = toolRegistry;
        this.observer = observer;
    }

    @Override
    public String run(String input) {

        List<LlmMessage> messages = new ArrayList<>();

        messages.add(LlmMessage.system("""
                You are ForgeMind, a software development agent.

                Use the available tools whenever you need information
                from the local project.

                Do not guess file contents.
                """));

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

                long toolStart = System.nanoTime();
                
                ToolResult result = tool.execute(
                        call.arguments()
                );

                Duration toolDuration = Duration.ofNanos(
                        System.nanoTime() - toolStart
                );

                observer.onToolResult(
                        call,
                        result,
                        toolDuration
                );
                
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
}