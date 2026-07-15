package com.trading.intelligence.infrastructure;

import com.trading.intelligence.domain.model.RawNewsArticle;
import com.trading.intelligence.domain.port.out.NewsProviderPort;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic {@link NewsProviderPort} stub for the {@code mock} profile.
 * Produces a handful of ticker-specific and macro articles so the news-scan
 * flow (fetch → classify → persist → view) works fully offline.
 */
@Primary
@Profile("mock")
@Component
public class MockNewsProvider implements NewsProviderPort {

    @Override
    public List<RawNewsArticle> fetchForTicker(String ticker, int limit) {
        List<RawNewsArticle> out = new ArrayList<>();
        String[] templates = {
                "%s beats quarterly earnings expectations",
                "Analysts raise price target on %s",
                "%s announces new product line",
                "%s faces supply-chain headwinds",
                "Institutional investors increase %s holdings"
        };
        int n = Math.min(limit, templates.length);
        for (int i = 0; i < n; i++) {
            String headline = templates[i].formatted(ticker);
            out.add(new RawNewsArticle(
                    headline,
                    "MockWire",
                    "https://mock.news/" + ticker + "/" + i,
                    headline + ". This is mock article content for offline testing of the "
                            + ticker + " news pipeline.",
                    Instant.now().minus(i + 1L, ChronoUnit.HOURS)));
        }
        return out;
    }

    @Override
    public List<RawNewsArticle> fetchMacroNews(int limit) {
        List<RawNewsArticle> out = new ArrayList<>();
        String[] headlines = {
                "Federal Reserve holds interest rates steady",
                "Inflation cools more than expected in latest report",
                "FOMC minutes signal cautious optimism",
                "US economy adds jobs above forecast",
                "Markets digest fresh economic data"
        };
        int n = Math.min(limit, headlines.length);
        for (int i = 0; i < n; i++) {
            out.add(new RawNewsArticle(
                    headlines[i],
                    "MockMacroWire",
                    "https://mock.news/macro/" + i,
                    headlines[i] + ". Mock macro article content for offline testing.",
                    Instant.now().minus(i + 1L, ChronoUnit.HOURS)));
        }
        return out;
    }
}
