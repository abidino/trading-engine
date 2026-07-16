package com.trading.marketdata.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Shared HTTP client for the free Yahoo Finance chart API. Centralises the
 * browser-like {@code User-Agent} and a small retry/backoff policy for HTTP 429
 * ("Too Many Requests"), which Yahoo returns aggressively for datacenter/cloud IPs.
 */
@Slf4j
@Component
public class YahooChartClient {

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    private final RestClient restClient;
    private final int maxRetries;
    private final long backoffMs;

    public YahooChartClient(
            @Value("${yahoo.base-url:https://query1.finance.yahoo.com}") String baseUrl,
            @Value("${yahoo.user-agent:}") String userAgent,
            @Value("${yahoo.max-retries:3}") int maxRetries,
            @Value("${yahoo.retry-backoff-ms:600}") long backoffMs) {
        this.maxRetries = Math.max(1, maxRetries);
        this.backoffMs = Math.max(0, backoffMs);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent == null || userAgent.isBlank()
                        ? DEFAULT_USER_AGENT : userAgent)
                .defaultHeader("Accept", "application/json,text/plain,*/*")
                .defaultHeader("Accept-Language", "en-US,en;q=0.9")
                .build();
    }

    /**
     * GETs the given chart URI as raw JSON, retrying with linear backoff when Yahoo
     * responds with HTTP 429. Other errors propagate immediately.
     */
    public String get(String uriTemplate, Object... uriVars) {
        HttpClientErrorException.TooManyRequests last = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return restClient.get()
                        .uri(uriTemplate, uriVars)
                        .retrieve()
                        .body(String.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                last = e;
                if (attempt < maxRetries) {
                    long sleep = backoffMs * attempt;
                    log.warn("Yahoo 429, retrying (attempt {}/{}) after {}ms", attempt, maxRetries, sleep);
                    sleepQuietly(sleep);
                }
            }
        }
        throw last;
    }

    private void sleepQuietly(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
