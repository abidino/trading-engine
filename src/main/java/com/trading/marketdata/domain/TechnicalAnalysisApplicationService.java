package com.trading.marketdata.domain;

import com.trading.marketdata.domain.model.PriceCandle;
import com.trading.marketdata.domain.model.SupportResistanceLevels;
import com.trading.marketdata.domain.model.TechnicalIndicatorSnapshot;
import com.trading.marketdata.domain.model.TrendAnalysis;
import com.trading.marketdata.domain.model.TrendDirection;
import com.trading.marketdata.domain.port.in.TechnicalAnalysisUseCase;
import com.trading.marketdata.domain.port.out.MarketDataProviderPort;
import com.trading.marketdata.domain.port.out.PriceCandleRepository;
import com.trading.marketdata.domain.port.out.TrendAnalysisRepository;
import com.trading.marketdata.domain.service.SupportResistanceCalculator;
import com.trading.marketdata.domain.service.TechnicalIndicatorCalculator;
import com.trading.marketdata.domain.service.TrendPromptAssembler;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.event.TechnicalTrendComputed;
import com.trading.shared.kernel.llm.LlmPort;
import com.trading.shared.kernel.llm.LlmRequest;
import com.trading.shared.kernel.llm.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for the technical-analysis capability of the marketdata module.
 *
 * Pipeline (per ticker):
 *  1. Sync recent OHLCV candles from the provider (dedup against the local cache).
 *  2. Compute the indicator snapshot (EMA/RSI/MACD) — {@link TechnicalIndicatorCalculator}.
 *  3. (analyzeTrend only) Ask the LLM to classify the trend — {@link TrendPromptAssembler} + {@link LlmPort}.
 *  4. Persist the verdict (idempotent per ticker+day) and publish a domain event.
 */
@Slf4j
@Service
@Transactional
public class TechnicalAnalysisApplicationService implements TechnicalAnalysisUseCase {

    private final MarketDataProviderPort providerPort;
    private final PriceCandleRepository candleRepository;
    private final TrendAnalysisRepository trendRepository;
    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final TechnicalIndicatorCalculator calculator = new TechnicalIndicatorCalculator();
    private final SupportResistanceCalculator srCalculator = new SupportResistanceCalculator();
    private final TrendPromptAssembler promptAssembler = new TrendPromptAssembler();

    /** How many previous daily verdicts to feed the LLM for continuity. */
    private static final int HISTORY_FOR_PROMPT = 30;

    public TechnicalAnalysisApplicationService(
            MarketDataProviderPort providerPort,
            PriceCandleRepository candleRepository,
            TrendAnalysisRepository trendRepository,
            LlmPort llmPort,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher) {
        this.providerPort = providerPort;
        this.candleRepository = candleRepository;
        this.trendRepository = trendRepository;
        this.llmPort = llmPort;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    // -----------------------------------------------------------------------

    @Override
    public TechnicalIndicatorSnapshot computeIndicators(String ticker, int lookbackDays) {
        String symbol = ticker.toUpperCase();
        return calculator.calculate(symbol, loadCandles(symbol, lookbackDays));
    }

    @Override
    public SupportResistanceLevels supportResistance(String ticker, int lookbackDays) {
        String symbol = ticker.toUpperCase();
        return srCalculator.calculate(symbol, loadCandles(symbol, lookbackDays));
    }

    /** Syncs recent candles then returns them, falling back to any cached history. */
    private List<PriceCandle> loadCandles(String symbol, int lookbackDays) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(lookbackDays);

        syncCandles(symbol, from, to);

        List<PriceCandle> candles = candleRepository.findByTickerAndDateBetween(symbol, from, to);
        if (candles.isEmpty()) {
            // Fall back to whatever history we have cached for the ticker.
            candles = candleRepository.findByTicker(symbol);
        }
        if (candles.isEmpty()) {
            throw new IllegalStateException("No price data available for " + symbol);
        }
        return candles;
    }

    @Override
    public TrendAnalysis analyzeTrend(String ticker, int lookbackDays) {
        String symbol = ticker.toUpperCase();
        TechnicalIndicatorSnapshot snapshot = computeIndicators(symbol, lookbackDays);

        List<TrendAnalysis> history = trendRepository.findRecentByTicker(symbol, HISTORY_FOR_PROMPT);

        LlmRequest request = promptAssembler.assemble(snapshot, history);
        LlmResponse response = llmPort.complete(request);
        Verdict verdict = parseVerdict(response.content());

        LocalDate today = LocalDate.now();
        UUID id = trendRepository.findByTickerAndDate(symbol, today)
                .map(TrendAnalysis::id)
                .orElse(UUID.randomUUID());

        TrendAnalysis analysis = new TrendAnalysis(
                id, symbol, today, verdict.trend(), verdict.confidence(),
                verdict.reasoning(), snapshot, response.modelUsed(), Instant.now());

        TrendAnalysis saved = trendRepository.save(analysis);
        log.info("Trend analysis for {}: {} (conf {}) — {} candles",
                symbol, saved.trend(), saved.confidence(), snapshot.dataPoints());

        eventPublisher.publishEvent(TechnicalTrendComputed.of(
                new Ticker(symbol), saved.analysisDate(), saved.trend().name(),
                saved.confidence(), saved.reasoning()));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrendAnalysis> latestTrend(String ticker) {
        return trendRepository.findLatestByTicker(ticker.toUpperCase());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrendAnalysis> trendHistory(String ticker, int limit) {
        return trendRepository.findRecentByTicker(ticker.toUpperCase(), limit);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Fetches fresh candles and persists only dates not already cached (avoids unique-constraint clashes). */
    private void syncCandles(String ticker, LocalDate from, LocalDate to) {
        List<PriceCandle> fresh;
        try {
            fresh = providerPort.fetchCandles(ticker, from, to);
        } catch (Exception e) {
            log.warn("Provider fetch failed for {} — using cached candles. Cause: {}", ticker, e.getMessage());
            return;
        }
        if (fresh.isEmpty()) {
            return;
        }
        Set<LocalDate> existing = new HashSet<>();
        candleRepository.findByTickerAndDateBetween(ticker, from, to)
                .forEach(c -> existing.add(c.candleDate()));

        List<PriceCandle> toPersist = fresh.stream()
                .filter(c -> !existing.contains(c.candleDate()))
                .toList();
        if (!toPersist.isEmpty()) {
            candleRepository.saveAll(toPersist);
            log.debug("Persisted {} new candles for {}", toPersist.size(), ticker);
        }
    }

    private Verdict parseVerdict(String raw) {
        try {
            String json = extractJson(raw);
            JsonNode node = objectMapper.readTree(json);
            TrendDirection trend = TrendDirection.valueOf(
                    node.get("trend").asString().trim().toUpperCase());
            double confidence = node.path("confidence").asDouble(0.5);
            String reasoning = node.path("reasoning").asString("");
            return new Verdict(trend, clamp(confidence), reasoning);
        } catch (Exception e) {
            log.warn("Failed to parse trend verdict, defaulting to SIDEWAYS. Raw: {}", raw);
            return new Verdict(TrendDirection.SIDEWAYS, 0.0,
                    "Could not parse LLM response: " + raw);
        }
    }

    /** Strips markdown fences / surrounding prose by taking the outermost JSON object. */
    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private record Verdict(TrendDirection trend, double confidence, String reasoning) {}
}
