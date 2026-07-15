package com.trading.orchestration.infrastructure;

import com.trading.shared.kernel.llm.LlmPort;
import com.trading.shared.kernel.llm.LlmRequest;
import com.trading.shared.kernel.llm.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Calls the Anthropic Messages API.
 */
@Slf4j
@Component("anthropicLlm")
public class AnthropicLlmAdapter implements LlmPort {

    private static final String BASE_URL = "https://api.anthropic.com/v1";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final LlmProperties.ProviderConfig config;
    private final ObjectMapper objectMapper;

    public AnthropicLlmAdapter(LlmProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getProvider("anthropic");
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-api-key", config.getApiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String model = request.modelOverride() != null ? request.modelOverride() : config.getModel();
        Instant start = Instant.now();

        // Anthropic uses a separate "system" field, not a system role message
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", request.maxTokens(),
                "system", request.systemPrompt(),
                "messages", List.of(
                        Map.of("role", "user", "content", request.userPrompt())
                )
        );

        try {
            String raw = restClient.post()
                    .uri("/messages")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("content").path(0).path("text").asString("");
            int inputTokens = root.path("usage").path("input_tokens").asInt(0);
            int outputTokens = root.path("usage").path("output_tokens").asInt(0);
            return new LlmResponse(content, model, inputTokens + outputTokens,
                    Duration.between(start, Instant.now()));
        } catch (Exception e) {
            throw new RuntimeException("Anthropic LLM call failed: " + e.getMessage(), e);
        }
    }
}
