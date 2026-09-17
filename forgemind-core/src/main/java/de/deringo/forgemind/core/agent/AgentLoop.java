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

        String systemPrompt = """
        You are ForgeMind, a software development agent.

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

        Clearly distinguish verified facts from assumptions.
        """;
        
        
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