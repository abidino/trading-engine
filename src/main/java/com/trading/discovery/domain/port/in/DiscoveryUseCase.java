package com.trading.discovery.domain.port.in;

import com.trading.discovery.domain.model.DiscoveryCandidate;
import com.trading.discovery.domain.model.DiscoveryFilter;
import com.trading.discovery.domain.model.SavedFilter;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port: discovery use cases.
 *
 * A discovery cycle screens the market, evaluates each candidate's technical
 * posture with the LLM, and persists a recommendation. The user can then promote
 * (→ watchlist) or dismiss (→ permanent blocklist, never re-recommended).
 */
public interface DiscoveryUseCase {

    /** Runs one screen + LLM evaluation pass for the given filter; persists results. */
    List<DiscoveryCandidate> runDiscoveryCycle(DiscoveryFilter filter);

    /** Runs all active saved filters (falls back to defaults when none are active). */
    List<DiscoveryCandidate> runActiveSavedFilters();

    /** On-demand evaluation of a single ticker (API-triggered). */
    DiscoveryCandidate evaluateTicker(String ticker);

    /** Current recommendations (RECOMMENDED status), excluding dismissed tickers. */
    List<DiscoveryCandidate> listRecommendations();

    /** Accept a recommendation: mark PROMOTED and publish AddToWatchlistRecommended. */
    void promoteTicker(String ticker);

    /** Reject: mark DISMISSED and add to the permanent blocklist. */
    void dismissTicker(String ticker);

    /** Tickers on the permanent blocklist. */
    List<String> listDismissed();

    /** Remove a ticker from the blocklist so it can be discovered again. */
    void undismissTicker(String ticker);

    // --- saved filters ---
    List<SavedFilter> listFilters();
    SavedFilter saveFilter(String name, String description, DiscoveryFilter criteria);
    void activateFilter(UUID id);
    void deactivateFilter(UUID id);
    void deleteFilter(UUID id);
}
