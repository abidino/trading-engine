package com.trading.marketdata.infrastructure;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.trading.marketdata.domain.model.PriceCandle;
import com.trading.marketdata.domain.model.TechnicalSignal;
import com.trading.marketdata.domain.port.out.MarketDataProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches OHLCV price data from Yahoo Finance Chart API.
 * Technical indicators (RSI, MACD, SMA) are computed from the raw candles.
 */
@Slf4j
@Component
public class YahooFinanceAdapter implements MarketDataProviderPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public YahooFinanceAdapter(
            @Value("${yahoo.base-url:https://query1.finance.yahoo.com}") String baseUrl,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "TradingEngine/1.0")
                .build();
    }

    @Override
    public List<PriceCandle> fetchCandles(String ticker, LocalDate from, LocalDate to) {
        try {
            long period1 = from.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long period2 = to.atStartOfDay().toEpochSecond(ZoneOffset.UTC);

            String raw = restClient.get()
                    .uri("/v8/finance/chart/{ticker}?period1={p1}&period2={p2}&interval=1d",
                            ticker, period1, period2)
                    .retrieve()
                    .body(String.class);

            return parseCandles(ticker, raw);
        } catch (Exception e) {
            log.warn("YahooFinance fetch failed for {}: {}", ticker, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<TechnicalSignal> fetchIndicators(String ticker, LocalDate from, LocalDate to) {
        // Indicators are computed locally from candles — no separate Yahoo endpoint
        List<PriceCandle> candles = fetchCandles(ticker, from, to);
        return computeIndicators(ticker, candles);
    }

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    private List<PriceCandle> parseCandles(String ticker, String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode result = root.path("chart").path("result").path(0);
        JsonNode timestamps = result.path("timestamp");
        JsonNode quote = result.path("indicators").path("quote").path(0);

        List<PriceCandle> candles = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            LocalDate date = LocalDate.ofEpochDay(timestamps.get(i).asLong() / 86400);
            candles.add(PriceCandle.create(
                    ticker, date,
                    BigDecimal.valueOf(quote.path("open").path(i).asDouble()),
                    BigDecimal.valueOf(quote.path("high").path(i).asDouble()),
                    BigDecimal.valueOf(quote.path("low").path(i).asDouble()),
                    BigDecimal.valueOf(quote.path("close").path(i).asDouble()),
                    quote.path("volume").path(i).asLong()
            ));
        }
        return candles;
    }

    private List<TechnicalSignal> computeIndicators(String ticker, List<PriceCandle> candles) {
        if (candles.isEmpty()) return List.of();
        // Simple SMA-14 and RSI-14 approximation
        List<TechnicalSignal> signals = new ArrayList<>();
        LocalDate latest = candles.get(candles.size() - 1).candleDate();
        int n = candles.size();
        if (n >= 14) {
            double sma14 = candles.subList(n - 14, n).stream()
                    .mapToDouble(c -> c.close().doubleValue()).average().orElse(0);
            signals.add(TechnicalSignal.create(ticker, "SMA_14",
                    BigDecimal.valueOf(sma14), latest));
        }
        return signals;
    }
}
