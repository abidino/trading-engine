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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls a local, OpenAI-compatible proxy at {@code llm.providers.proxy.base-url}
 * (e.g. {@code http://localhost:4655/v1}).
 *
 * <p>Kept deliberately permissive for maximum compatibility with lightweight proxies:
 * it does NOT force {@code response_format=json_object} (many proxies don't implement it),
 * relying instead on the prompt's JSON instructions plus the orchestration parser's
 * markdown-fence stripping. The {@code Authorization} header is only sent when an API key
 * is configured.</p>
 */
@Slf4j
@Component("proxyLlm")
public class ProxyLlmAdapter implements LlmPort {

    private final RestClient restClient;
    private final LlmProperties.ProviderConfig config;
    private final ObjectMapper objectMapper;

    public ProxyLlmAdapter(LlmProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getProvider("proxy");
        this.objectMapper = objectMapper;

        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank()
                ? config.getBaseUrl()
                : "http://localhost:4655/v1";
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json");
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + config.getApiKey());
        }
        this.restClient = builder.build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String model = request.modelOverride() != null ? request.modelOverride() : config.getModel();
        Instant start = Instant.now();

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", request.systemPrompt()),
                Map.of("role", "user", "content", request.userPrompt())
        ));
        body.put("temperature", request.temperature());
        body.put("max_tokens", request.maxTokens());
        body.put("stream", false);

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
            throw new RuntimeException("Proxy LLM call failed: " + e.getMessage(), e);
        }
    }
}
