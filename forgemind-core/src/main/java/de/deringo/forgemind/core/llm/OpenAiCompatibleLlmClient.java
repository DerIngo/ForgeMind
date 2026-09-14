package de.deringo.forgemind.core.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
            Message[] messages = request.messages().stream()
                    .map(message -> new Message(
                            message.role().name().toLowerCase(),
                            message.content()
                    ))
                    .toArray(Message[]::new);

            String body = objectMapper.writeValueAsString(
                    new ChatCompletionRequest(
                            request.model(),
                            messages
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

            String content = root
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            return new LlmResponse(content);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("LLM request failed", e);
        }
    }

    private record ChatCompletionRequest(
            String model,
            Message[] messages
    ) {
    }

    private record Message(
            String role,
            String content
    ) {
    }
}