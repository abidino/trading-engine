package com.trading.orchestration.infrastructure;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.trading.shared.kernel.llm.LlmPort;
import com.trading.shared.kernel.llm.LlmRequest;
import com.trading.shared.kernel.llm.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Calls Ollama's OpenAI-compatible /v1/chat/completions endpoint.
 * Ollama must expose the OpenAI-compatible API (default when started normally).
 */
@Slf4j
@Component("ollamaLlm")
public class OllamaLlmAdapter implements LlmPort {

    private final RestClient restClient;
    private final LlmProperties.ProviderConfig config;
    private final ObjectMapper objectMapper;

    public OllamaLlmAdapter(LlmProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getProvider("ollama");
        this.objectMapper = objectMapper;
        String baseUrl = (config.getBaseUrl() != null ? config.getBaseUrl() : "http://localhost:11434");
        // Ensure we target the OpenAI-compatible endpoint
        if (!baseUrl.endsWith("/v1")) {
            baseUrl = baseUrl.replaceAll("/v1/?$", "") + "/v1";
        }
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String model = request.modelOverride() != null ? request.modelOverride() : config.getModel();
        Instant start = Instant.now();

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())
                ),
                "temperature", request.temperature(),
                "stream", false
        );

        try {
            String raw = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").path(0).path("message").path("content").asString("");
            int tokens = root.path("usage").path("total_tokens").asInt(0);
            return new LlmResponse(content, model, tokens, Duration.between(start, Instant.now()));
        } catch (Exception e) {
            throw new RuntimeException("Ollama LLM call failed: " + e.getMessage(), e);
        }
    }
}
