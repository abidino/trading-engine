package com.trading.orchestration.domain;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates analysis triggering so the same ticker is never analysed twice at once and
 * event-driven triggers (news arrival, support/resistance touch) don't flood the pipeline.
 *
 * <ul>
 *   <li><b>In-flight guard</b> — {@link #beginIfAbsent(String)} atomically marks a ticker as
 *       being analysed; a second concurrent request for the same ticker is skipped until
 *       {@link #end(String)} is called.</li>
 *   <li><b>Cooldown</b> — {@link #cooldownElapsed(String, Duration)} lets event triggers avoid
 *       re-analysing a ticker that was just analysed. Scheduled full sweeps and manual UI
 *       triggers bypass the cooldown (they only respect the in-flight guard).</li>
 * </ul>
 */
@Component
public class AnalysisTriggerCoordinator {

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> lastCompleted = new ConcurrentHashMap<>();

    /** Atomically marks the ticker in-flight. Returns {@code false} if already running. */
    public boolean beginIfAbsent(String ticker) {
        return inFlight.add(normalize(ticker));
    }

    /** Clears the in-flight mark and records completion time for cooldown checks. */
    public void end(String ticker) {
        String key = normalize(ticker);
        inFlight.remove(key);
        lastCompleted.put(key, Instant.now());
    }

    /** True when no analysis has completed for the ticker within the given window. */
    public boolean cooldownElapsed(String ticker, Duration cooldown) {
        Instant last = lastCompleted.get(normalize(ticker));
        return last == null || Duration.between(last, Instant.now()).compareTo(cooldown) >= 0;
    }

    public boolean isInFlight(String ticker) {
        return inFlight.contains(normalize(ticker));
    }

    private String normalize(String ticker) {
        return ticker == null ? "" : ticker.trim().toUpperCase();
    }
}
