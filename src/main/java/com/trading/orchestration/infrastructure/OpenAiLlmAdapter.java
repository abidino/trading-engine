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

/** Calls OpenAI /v1/chat/completions with JSON mode enforced. */
@Slf4j
@Component("openaiLlm")
public class OpenAiLlmAdapter implements LlmPort {

    private static final String BASE_URL = "https://api.openai.com/v1";

    private final RestClient restClient;
    private final LlmProperties.ProviderConfig config;
    private final ObjectMapper objectMapper;

    public OpenAiLlmAdapter(LlmProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getProvider("openai");
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
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
                "max_tokens", request.maxTokens(),
                "response_format", Map.of("type", "json_object")
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
            throw new RuntimeException("OpenAI LLM call failed: " + e.getMessage(), e);
        }
    }
}
