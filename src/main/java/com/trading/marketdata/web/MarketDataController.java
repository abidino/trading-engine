package com.trading.marketdata.web;

import com.trading.marketdata.domain.MarketDataApplicationServiceImpl;
import com.trading.marketdata.domain.port.in.IntradayQuoteUseCase;
import com.trading.marketdata.domain.port.in.TechnicalAnalysisUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataApplicationServiceImpl service;
    private final TechnicalAnalysisUseCase technicalAnalysis;
    private final IntradayQuoteUseCase intradayQuotes;

    @GetMapping("/{ticker}/candles")
    public ResponseEntity<List<PriceCandleResponse>> getCandles(@PathVariable String ticker) {
        return ResponseEntity.ok(service.getCachedCandles(ticker)
                .stream().map(PriceCandleResponse::from).toList());
    }

    @PostMapping("/{ticker}/sync")
    public ResponseEntity<Void> syncTicker(@PathVariable String ticker,
                                           @RequestParam(defaultValue = "90") int days) {
        service.syncAndGetCandles(ticker, days);
        return ResponseEntity.ok().build();
    }

    // -----------------------------------------------------------------------
    // Technical analysis
    // -----------------------------------------------------------------------

    /** Compute indicators on demand (syncs candles, no LLM, no persistence). */
    @GetMapping("/{ticker}/indicators")
    public ResponseEntity<TechnicalIndicatorResponse> getIndicators(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "400") int days) {
        return ResponseEntity.ok(
                TechnicalIndicatorResponse.from(technicalAnalysis.computeIndicators(ticker, days)));
    }

    /** Compute support &amp; resistance levels on demand (syncs candles, no LLM, no persistence). */
    @GetMapping("/{ticker}/support-resistance")
    public ResponseEntity<SupportResistanceResponse> getSupportResistance(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "400") int days) {
        return ResponseEntity.ok(
                SupportResistanceResponse.from(technicalAnalysis.supportResistance(ticker, days)));
    }

    /** Manually trigger a full trend analysis (compute + LLM verdict + persist). */
    @PostMapping("/{ticker}/analyze-trend")
    public ResponseEntity<TrendAnalysisResponse> analyzeTrend(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "400") int days) {
        return ResponseEntity.ok(
                TrendAnalysisResponse.from(technicalAnalysis.analyzeTrend(ticker, days)));
    }

    /** Latest persisted trend verdict for the ticker. */
    @GetMapping("/{ticker}/trend")
    public ResponseEntity<TrendAnalysisResponse> getLatestTrend(@PathVariable String ticker) {
        return technicalAnalysis.latestTrend(ticker)
                .map(TrendAnalysisResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Recent trend-verdict history, newest first. */
    @GetMapping("/{ticker}/trends")
    public ResponseEntity<List<TrendAnalysisResponse>> getTrendHistory(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(technicalAnalysis.trendHistory(ticker, limit)
                .stream().map(TrendAnalysisResponse::from).toList());
    }

    // -----------------------------------------------------------------------
    // Intraday quotes (pre-market / regular / post-market)
    // -----------------------------------------------------------------------

    /** Fetch the live quote now and persist it. */
    @PostMapping("/{ticker}/quote/refresh")
    public ResponseEntity<IntradayQuoteResponse> refreshQuote(@PathVariable String ticker) {
        return ResponseEntity.ok(IntradayQuoteResponse.from(intradayQuotes.refreshQuote(ticker)));
    }

    /** Latest persisted intraday quote for the ticker. */
    @GetMapping("/{ticker}/quote")
    public ResponseEntity<IntradayQuoteResponse> getLatestQuote(@PathVariable String ticker) {
        return intradayQuotes.latestQuote(ticker)
                .map(IntradayQuoteResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Intraday quotes captured within the last {@code minutes}, newest first. */
    @GetMapping("/{ticker}/quotes")
    public ResponseEntity<List<IntradayQuoteResponse>> getQuoteHistory(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "390") int minutes) {
        return ResponseEntity.ok(intradayQuotes.quoteHistory(ticker, minutes)
                .stream().map(IntradayQuoteResponse::from).toList());
    }
}