package com.trading.orchestration.infrastructure;

import com.trading.shared.kernel.AnalysisRequestType;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calls the DeepSeek API (OpenAI-compatible) at {@code llm.providers.deepseek.base-url}
 * (default {@code https://api.deepseek.com}).
 *
 * <p><b>Cost-aware thinking mode.</b> DeepSeek's thinking mode emits a chain-of-thought that
 * is billed as output tokens; it materially improves decision quality but is wasteful for the
 * high-volume, low-stakes calls (news/social summaries, technical-trend, classification).
 * This adapter therefore enables thinking <em>only</em> for the request types listed in
 * {@code llm.providers.deepseek.thinking-types} (defaults to the three decision types), and
 * runs everything else in non-thinking mode. All of this is tunable from YAML — no code change
 * needed to shift the thinking/non-thinking boundary.</p>
 *
 * <pre>
 * llm:
 *   providers:
 *     deepseek:
 *       base-url: https://api.deepseek.com
 *       api-key: ${DEEPSEEK_API_KEY:}
 *       model: deepseek-v4-flash
 *       thinking-enabled: true
 *       thinking-types: PORTFOLIO_REVIEW,WATCHLIST_REVIEW,DISCOVERY
 *       reasoning-effort: high
 * </pre>
 *
 * <p>The final answer is read from {@code choices[0].message.content}; DeepSeek returns the
 * chain-of-thought separately in {@code reasoning_content}, which we intentionally ignore.</p>
 */
@Slf4j
@Component("deepseekLlm")
public class DeepSeekLlmAdapter implements LlmPort {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";

    private final RestClient restClient;
    private final LlmProperties.ProviderConfig config;
    private final ObjectMapper objectMapper;
    private final Set<String> thinkingTypes;

    public DeepSeekLlmAdapter(LlmProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getProvider("deepseek");
        this.objectMapper = objectMapper;
        this.thinkingTypes = parseTypes(config.getThinkingTypes());

        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank()
                ? config.getBaseUrl()
                : DEFAULT_BASE_URL;
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
        boolean thinking = useThinking(request.analysisType());
        Instant start = Instant.now();

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", request.systemPrompt()),
                Map.of("role", "user", "content", request.userPrompt())
        ));
        body.put("max_tokens", request.maxTokens());
        body.put("stream", false);
        if (config.isJsonMode() && wantsJsonResponse(request)) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        if (thinking) {
            // Thinking mode ignores temperature/top_p, so we omit temperature here.
            body.put("thinking", Map.of("type", "enabled"));
            if (config.getReasoningEffort() != null && !config.getReasoningEffort().isBlank()) {
                body.put("reasoning_effort", config.getReasoningEffort());
            }
        } else {
            body.put("thinking", Map.of("type", "disabled"));
            body.put("temperature", request.temperature());
        }

        try {
            String raw = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").path(0).path("message").path("content").asString("");
            int tokens = root.path("usage").path("total_tokens").asInt(0);
            log.debug("DeepSeek call type={} thinking={} model={} tokens={}",
                    request.analysisType(), thinking, model, tokens);
            return new LlmResponse(content, model, tokens, Duration.between(start, Instant.now()));
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek LLM call failed: " + e.getMessage(), e);
        }
    }

    /** True when this request type should run in thinking mode (per YAML config). */
    private boolean useThinking(AnalysisRequestType type) {
        return config.isThinkingEnabled() && type != null && thinkingTypes.contains(type.name());
    }

    /**
     * DeepSeek (like OpenAI) rejects {@code response_format=json_object} unless the word
     * "json" appears in the prompt. Prose requests (e.g. news/social summaries) legitimately
     * omit it, so only enable JSON mode when the prompt actually asks for JSON output.
     */
    private static boolean wantsJsonResponse(LlmRequest request) {
        return mentionsJson(request.systemPrompt()) || mentionsJson(request.userPrompt());
    }

    private static boolean mentionsJson(String text) {
        return text != null && text.toLowerCase(Locale.ROOT).contains("json");
    }

    private static Set<String> parseTypes(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase())
                .collect(Collectors.toUnmodifiableSet());
    }
}
