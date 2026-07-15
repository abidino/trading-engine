package com.trading.intelligence.web;

import com.trading.intelligence.domain.IntelligenceApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/intelligence")
@RequiredArgsConstructor
public class IntelligenceController {

    private final IntelligenceApplicationService service;

    // -----------------------------------------------------------------------
    // News — queries
    // -----------------------------------------------------------------------

    /** News relevant to a ticker (incl. market-wide macro) for the last N days. */
    @GetMapping("/{ticker}/news")
    public ResponseEntity<List<NewsResponse>> getNews(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(service.getNewsForTicker(ticker, days)
                .stream().map(NewsResponse::from).toList());
    }

    /** Most recent news across all tickers. */
    @GetMapping("/news/recent")
    public ResponseEntity<List<NewsResponse>> getRecent(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(service.getRecentNews(limit)
                .stream().map(NewsResponse::from).toList());
    }

    // -----------------------------------------------------------------------
    // News — scanning (UI "scan now" buttons)
    // -----------------------------------------------------------------------

    /** Scan + classify news for a single ticker. */
    @PostMapping("/news/scan/{ticker}")
    public ResponseEntity<Map<String, Integer>> scanTicker(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "20") int limit) {
        int saved = service.scanTickerNews(ticker, limit);
        return ResponseEntity.ok(Map.of("newArticles", saved));
    }

    /** Scan + classify macro market news. */
    @PostMapping("/news/scan-macro")
    public ResponseEntity<Map<String, Integer>> scanMacro(
            @RequestParam(defaultValue = "30") int limit) {
        int saved = service.scanMacroNews(limit);
        return ResponseEntity.ok(Map.of("newArticles", saved));
    }

    // -----------------------------------------------------------------------
    // Social (structure only for now)
    // -----------------------------------------------------------------------

    @GetMapping("/{ticker}/social")
    public ResponseEntity<List<SocialResponse>> getSocial(@PathVariable String ticker) {
        return ResponseEntity.ok(service.getSocialSignalsByTicker(ticker)
                .stream().map(SocialResponse::from).toList());
    }

    @PostMapping("/{ticker}/social/collect")
    public ResponseEntity<Void> collectSocial(@PathVariable String ticker) {
        service.collectSocialSignalsForTicker(ticker, 10);
        return ResponseEntity.ok().build();
    }
}
