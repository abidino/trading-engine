package com.trading.discovery.web;

import com.trading.discovery.domain.model.DiscoveryCandidate;
import com.trading.discovery.domain.model.FilterCatalog;
import com.trading.discovery.domain.model.SavedFilter;
import com.trading.discovery.domain.port.in.DiscoveryUseCase;
import com.trading.discovery.infrastructure.FinvizFilterCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryUseCase discoveryUseCase;
    private final FinvizFilterCatalog filterCatalog;

    // -----------------------------------------------------------------------
    // Filter catalog (single source of truth for the UI dropdowns)
    // -----------------------------------------------------------------------

    /** Exact Finviz filter options (scraped) that drive the UI dropdowns + query mapping. */
    @GetMapping("/filter-options")
    public ResponseEntity<FilterCatalog> filterOptions() {
        return ResponseEntity.ok(filterCatalog.catalog());
    }

    // -----------------------------------------------------------------------
    // Recommendations
    // -----------------------------------------------------------------------

    @GetMapping("/stocks")
    public ResponseEntity<List<DiscoveredStockResponse>> listStocks() {
        return ResponseEntity.ok(discoveryUseCase.listRecommendations().stream()
                .map(DiscoveredStockResponse::from)
                .toList());
    }

    /** Alias kept for backward compatibility. */
    @GetMapping("/candidates")
    public ResponseEntity<List<DiscoveredStockResponse>> listCandidates() {
        return listStocks();
    }

    @PostMapping("/stocks/{ticker}/promote")
    public ResponseEntity<Void> promote(@PathVariable String ticker) {
        discoveryUseCase.promoteTicker(ticker);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stocks/{ticker}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable String ticker) {
        discoveryUseCase.dismissTicker(ticker);
        return ResponseEntity.ok().build();
    }

    /** On-demand evaluation of a single ticker. */
    @PostMapping("/evaluate/{ticker}")
    public ResponseEntity<DiscoveredStockResponse> evaluate(@PathVariable String ticker) {
        DiscoveryCandidate c = discoveryUseCase.evaluateTicker(ticker);
        return ResponseEntity.ok(DiscoveredStockResponse.from(c));
    }

    // -----------------------------------------------------------------------
    // Run
    // -----------------------------------------------------------------------

    /** Runs all active saved filters (falls back to defaults when none are active). */
    @PostMapping("/run")
    public ResponseEntity<RunResult> runCycle() {
        List<DiscoveryCandidate> recommendations = discoveryUseCase.runActiveSavedFilters();
        return ResponseEntity.ok(new RunResult(recommendations.size()));
    }

    /**
     * Ad-hoc run: screens + evaluates with the supplied criteria immediately, WITHOUT
     * persisting a saved filter. Returns the evaluated candidates directly so the UI
     * can show one-off results.
     */
    @PostMapping("/run/ad-hoc")
    public ResponseEntity<List<DiscoveredStockResponse>> runAdHoc(@RequestBody CreateFilterRequest req) {
        List<DiscoveryCandidate> results = discoveryUseCase.runDiscoveryCycle(req.toDiscoveryFilter());
        return ResponseEntity.ok(results.stream()
                .map(DiscoveredStockResponse::from)
                .toList());
    }

    // -----------------------------------------------------------------------
    // Dismiss blocklist
    // -----------------------------------------------------------------------

    @GetMapping("/dismissed")
    public ResponseEntity<List<String>> listDismissed() {
        return ResponseEntity.ok(discoveryUseCase.listDismissed());
    }

    @DeleteMapping("/dismissed/{ticker}")
    public ResponseEntity<Void> undismiss(@PathVariable String ticker) {
        discoveryUseCase.undismissTicker(ticker);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Saved filters (persistent)
    // -----------------------------------------------------------------------

    @GetMapping("/filters")
    public ResponseEntity<List<FilterRecord>> listFilters() {
        return ResponseEntity.ok(discoveryUseCase.listFilters().stream()
                .map(FilterRecord::from)
                .toList());
    }

    @PostMapping("/filters")
    public ResponseEntity<FilterRecord> createFilter(@RequestBody CreateFilterRequest req) {
        SavedFilter saved = discoveryUseCase.saveFilter(
                req.name(), req.description(), req.toDiscoveryFilter());
        return ResponseEntity.ok(FilterRecord.from(saved));
    }

    @PostMapping("/filters/{id}/activate")
    public ResponseEntity<Void> activateFilter(@PathVariable String id) {
        discoveryUseCase.activateFilter(UUID.fromString(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/filters/{id}/deactivate")
    public ResponseEntity<Void> deactivateFilter(@PathVariable String id) {
        discoveryUseCase.deactivateFilter(UUID.fromString(id));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/filters/{id}")
    public ResponseEntity<Void> deleteFilter(@PathVariable String id) {
        discoveryUseCase.deleteFilter(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }
}
