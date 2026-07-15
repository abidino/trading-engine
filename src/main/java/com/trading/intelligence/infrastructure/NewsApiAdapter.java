package com.trading.intelligence.infrastructure;

import com.trading.intelligence.domain.model.RawNewsArticle;
import com.trading.intelligence.domain.port.out.NewsProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches news from NewsAPI.org (free tier: /v2/everything).
 *
 * Returns raw articles; categorisation / sentiment happens later in the
 * application service via the LLM.
 */
@Slf4j
@Component
public class NewsApiAdapter implements NewsProviderPort {

    /** Macro query covering the main US market-moving themes. */
    private static final String MACRO_QUERY =
            "\"Federal Reserve\" OR \"interest rates\" OR inflation OR \"stock market\" OR economy OR FOMC";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public NewsApiAdapter(
            @Value("${newsapi.base-url:https://newsapi.org}") String baseUrl,
            @Value("${newsapi.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .build();
    }

    @Override
    public List<RawNewsArticle> fetchForTicker(String ticker, int limit) {
        try {
            String raw = restClient.get()
                    .uri("/v2/everything?q={ticker}&pageSize={limit}&sortBy=publishedAt&language=en",
                            ticker, limit)
                    .retrieve()
                    .body(String.class);
            return parse(raw);
        } catch (Exception e) {
            log.warn("NewsAPI fetch failed for {}: {}", ticker, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<RawNewsArticle> fetchMacroNews(int limit) {
        try {
            String raw = restClient.get()
                    .uri("/v2/everything?q={q}&pageSize={limit}&sortBy=publishedAt&language=en",
                            MACRO_QUERY, limit)
                    .retrieve()
                    .body(String.class);
            return parse(raw);
        } catch (Exception e) {
            log.warn("NewsAPI macro fetch failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RawNewsArticle> parse(String json) {
        List<RawNewsArticle> records = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return records;
        }
        JsonNode articles = objectMapper.readTree(json).path("articles");
        Instant now = Instant.now();

        for (JsonNode a : articles) {
            String headline = a.path("title").asString("");
            String source = a.path("source").path("name").asString("unknown");
            String url = a.path("url").asString("");
            String content = a.path("content").asString("");
            String publishedAtStr = a.path("publishedAt").asString("");
            Instant publishedAt = parseInstant(publishedAtStr, now);
            records.add(new RawNewsArticle(headline, source, url, content, publishedAt));
        }
        return records;
    }

    private Instant parseInstant(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
