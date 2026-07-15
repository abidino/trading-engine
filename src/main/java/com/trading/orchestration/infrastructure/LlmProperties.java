package com.trading.orchestration.infrastructure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Binds the {@code llm.*} section of application.yml.
 *
 * <pre>
 * llm:
 *   default-provider: ollama
 *   providers:
 *     ollama:
 *       base-url: http://localhost:11434
 *       model: llama3
 *     openai:
 *       api-key: sk-...
 *       model: gpt-4o
 *     anthropic:
 *       api-key: sk-ant-...
 *       model: claude-sonnet-4-6
 *   routing:
 *     PORTFOLIO_REVIEW: ollama
 *     WATCHLIST_REVIEW: ollama
 *     DISCOVERY: ollama
 *     NEWS_SUMMARY: ollama
 *     SOCIAL_SUMMARY: ollama
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String defaultProvider = "ollama";
    private Map<String, ProviderConfig> providers = new HashMap<>();
    /** Maps AnalysisRequestType.name() → provider key (ollama / openai / anthropic). */
    private Map<String, String> routing = new HashMap<>();

    @Data
    public static class ProviderConfig {
        private String baseUrl;
        private String apiKey;
        private String model;

        // ── DeepSeek-style thinking-mode controls (ignored by other providers) ──
        /** Master switch: when true, thinking mode is used for the request types listed below. */
        private boolean thinkingEnabled = false;
        /**
         * Comma-separated {@link com.trading.shared.kernel.AnalysisRequestType} names that should
         * run in thinking mode. Everything else (news/social summaries, technical-trend,
         * classification) stays non-thinking to keep cost/latency low.
         */
        private String thinkingTypes = "PORTFOLIO_REVIEW,WATCHLIST_REVIEW,DISCOVERY";
        /** Reasoning effort passed to the provider when thinking is enabled (high / max). */
        private String reasoningEffort = "high";
        /** Send {@code response_format=json_object}. Disable for providers that don't support it. */
        private boolean jsonMode = true;
    }

    public ProviderConfig getProvider(String key) {
        return providers.getOrDefault(key, new ProviderConfig());
    }
}
