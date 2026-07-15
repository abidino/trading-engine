package com.trading.intelligence.infrastructure;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.trading.intelligence.domain.model.SocialSignal;
import com.trading.intelligence.domain.port.out.SocialSignalProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetches Reddit posts from /r/stocks or /r/investing subreddits.
 * Uses the unauthenticated JSON API (rate-limited). For production, replace
 * with OAuth2 token flow.
 */
@Slf4j
@Component
public class RedditAdapter implements SocialSignalProviderPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RedditAdapter(
            @Value("${reddit.base-url:https://www.reddit.com}") String baseUrl,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "TradingEngine:v1.0 (by /u/tradingbot)")
                .build();
    }

    @Override
    public List<SocialSignal> fetchForTicker(String ticker, int limit) {
        try {
            String raw = restClient.get()
                    .uri("/r/stocks/search.json?q={ticker}&sort=new&limit={limit}", ticker, limit)
                    .retrieve()
                    .body(String.class);
            return parse(ticker, raw);
        } catch (Exception e) {
            log.warn("Reddit fetch failed for {}: {}", ticker, e.getMessage());
            return List.of();
        }
    }

    private List<SocialSignal> parse(String ticker, String json) throws Exception {
        JsonNode posts = objectMapper.readTree(json).path("data").path("children");
        List<SocialSignal> signals = new ArrayList<>();

        for (JsonNode post : posts) {
            JsonNode data = post.path("data");
            String title = data.path("title").asString("");
            String selftext = data.path("selftext").asString("");
            String content = title + (selftext.isBlank() ? "" : "\n" + selftext);
            double score = data.path("score").asDouble(0);
            double numComments = data.path("num_comments").asDouble(0);
            // Normalize engagement: log10(upvotes + comments + 1)
            double engagement = Math.log10(score + numComments + 1);

            signals.add(SocialSignal.create(ticker, "reddit", content, engagement, 0.0));
        }
        return signals;
    }
}
