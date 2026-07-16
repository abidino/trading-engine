package com.trading.marketdata.infrastructure;

import com.trading.marketdata.domain.model.IntradayQuote;
import com.trading.marketdata.domain.model.MarketSession;
import com.trading.marketdata.domain.port.out.IntradayQuoteProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Live intraday quote provider backed by the free Yahoo Finance chart API
 * ({@code includePrePost=true}). The {@code meta} block carries the current price,
 * previous close, volume and the pre/regular/post trading-period boundaries used to
 * derive the {@link MarketSession}.
 *
 * No API key required. Falls back to {@link Optional#empty()} on any error so the
 * caller (job or API) degrades gracefully.
 */
@Slf4j
@Component
public class YahooIntradayQuoteAdapter implements IntradayQuoteProviderPort {

    private final YahooChartClient chartClient;
    private final ObjectMapper objectMapper;

    public YahooIntradayQuoteAdapter(YahooChartClient chartClient, ObjectMapper objectMapper) {
        this.chartClient = chartClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<IntradayQuote> fetchQuote(String ticker) {
        try {
            String raw = chartClient.get(
                    "/v8/finance/chart/{ticker}?interval=1m&range=1d&includePrePost=true", ticker);
            return parse(ticker, raw);
        } catch (Exception e) {
            log.warn("Yahoo intraday quote fetch failed for {}: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<IntradayQuote> parse(String ticker, String json) {
        JsonNode meta = objectMapper.readTree(json)
                .path("chart").path("result").path(0).path("meta");
        if (meta.isMissingNode() || meta.path("regularMarketPrice").isMissingNode()) {
            return Optional.empty();
        }

        long now = Instant.now().getEpochSecond();
        JsonNode periods = meta.path("currentTradingPeriod");
        MarketSession session = resolveSession(periods, now);

        double price = meta.path("regularMarketPrice").asDouble(0);
        if (session == MarketSession.PRE_MARKET && meta.has("preMarketPrice")) {
            price = meta.path("preMarketPrice").asDouble(price);
        } else if (session == MarketSession.POST_MARKET && meta.has("postMarketPrice")) {
            price = meta.path("postMarketPrice").asDouble(price);
        }

        double previousClose = meta.path("chartPreviousClose")
                .asDouble(meta.path("previousClose").asDouble(0));
        long volume = meta.path("regularMarketVolume").asLong(0);
        long quoteEpoch = meta.path("regularMarketTime").asLong(now);

        IntradayQuote quote = IntradayQuote.create(
                ticker,
                session,
                BigDecimal.valueOf(price),
                previousClose > 0 ? BigDecimal.valueOf(previousClose) : null,
                volume,
                Instant.ofEpochSecond(quoteEpoch));
        return Optional.of(quote);
    }

    private MarketSession resolveSession(JsonNode periods, long now) {
        if (within(periods.path("pre"), now)) return MarketSession.PRE_MARKET;
        if (within(periods.path("regular"), now)) return MarketSession.REGULAR;
        if (within(periods.path("post"), now)) return MarketSession.POST_MARKET;
        return MarketSession.CLOSED;
    }

    private boolean within(JsonNode period, long now) {
        if (period.isMissingNode()) return false;
        long start = period.path("start").asLong(0);
        long end = period.path("end").asLong(0);
        return start > 0 && end > 0 && now >= start && now < end;
    }
}
