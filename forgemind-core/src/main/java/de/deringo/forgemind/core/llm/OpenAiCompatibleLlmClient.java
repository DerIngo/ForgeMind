package de.deringo.forgemind.core.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.deringo.forgemind.core.tool.ToolCall;

public final class OpenAiCompatibleLlmClient implements LlmClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final String apiKey;

    public OpenAiCompatibleLlmClient(
            String baseUrl,
            String apiKey
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUri = URI.create(baseUrl);
        this.apiKey = apiKey;
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        try {
            ToolDefinitionDto[] tools = request.tools()
                    .stream()
                    .map(tool -> new ToolDefinitionDto(
                            "function",
                            new FunctionDefinition(
                                    tool.name(),
                                    tool.description(),
                                    tool.parameters()
                            )
                    ))
                    .toArray(ToolDefinitionDto[]::new);
            
            Message[] messages = request.messages()
                    .stream()
                    .map(this::toMessage)
                    .toArray(Message[]::new);

            String body = objectMapper.writeValueAsString(
                    new ChatCompletionRequest(
                            request.model(),
                            messages,
                            tools
                    )
            );

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(baseUri.resolve("/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                        "LLM request failed: "
                                + response.statusCode()
                                + " "
                                + response.body()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());

            JsonNode message = root
                    .path("choices")
                    .path(0)
                    .path("message");

            String content = message.path("content").asText("");

            List<ToolCall> toolCalls = new ArrayList<>();

            for (JsonNode node : message.path("tool_calls")) {

                JsonNode function = node.path("function");

                Map<String, Object> arguments =
                        objectMapper.readValue(
                                function.path("arguments").asText(),
                                new TypeReference<Map<String, Object>>() {}
                        );

                toolCalls.add(
                        new ToolCall(
                                node.path("id").asText(),
                                function.path("name").asText(),
                                arguments
                        )
                );
            }

            return new LlmResponse(content, toolCalls);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("LLM request failed", e);
        }
    }

    private Message toMessage(LlmMessage message) {

        List<ToolCallDto> toolCalls = message.toolCalls()
                .stream()
                .map(call -> new ToolCallDto(
                        call.id(),
                        "function",
                        new FunctionCallDto(
                                call.name(),
                                writeJson(call.arguments())
                        )
                ))
                .toList();

        return new Message(
                message.role().name().toLowerCase(),
                message.content(),
                toolCalls.isEmpty() ? null : toolCalls,
                message.toolCallId()
        );
    }
    
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not serialize JSON",
                    e
            );
        }
    }

    private record ChatCompletionRequest(
            String model,
            Message[] messages,
            ToolDefinitionDto[] tools
    ) {}

    private record Message(
            String role,
            String content,
            List<ToolCallDto> tool_calls,
            String tool_call_id
    ) {
    }
    
    private record FunctionDefinition(
            String name,
            String description,
            Map<String, Object> parameters
    ) {}

    private record ToolDefinitionDto(
            String type,
            FunctionDefinition function
    ) {}
    
    private record ToolCallDto(
            String id,
            String type,
            FunctionCallDto function
    ) {
    }

    private record FunctionCallDto(
            String name,
            String arguments
    ) {
    }
}