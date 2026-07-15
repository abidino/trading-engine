package com.trading.orchestration.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for the trigger coordinator that keeps the analysis pipeline calm under many
 * event-driven triggers (4-hourly sweep, news arrival, price/S-R touch, watchlist add).
 */
class AnalysisTriggerCoordinatorTest {

    private final AnalysisTriggerCoordinator coordinator = new AnalysisTriggerCoordinator();

    @Test
    void inFlightGuardBlocksConcurrentSameTicker() {
        assertTrue(coordinator.beginIfAbsent("NVDA"));
        assertFalse(coordinator.beginIfAbsent("NVDA"), "second concurrent begin must be blocked");
        assertTrue(coordinator.isInFlight("nvda"), "guard is case-insensitive");
        coordinator.end("NVDA");
        assertFalse(coordinator.isInFlight("NVDA"));
        assertTrue(coordinator.beginIfAbsent("NVDA"), "after end, ticker can run again");
    }

    @Test
    void cooldownElapsedTrueBeforeAnyRun() {
        assertTrue(coordinator.cooldownElapsed("AAPL", Duration.ofMinutes(90)));
    }

    @Test
    void cooldownNotElapsedRightAfterCompletion() {
        coordinator.beginIfAbsent("LITE");
        coordinator.end("LITE"); // records completion "now"
        assertFalse(coordinator.cooldownElapsed("LITE", Duration.ofMinutes(90)));
        assertTrue(coordinator.cooldownElapsed("LITE", Duration.ZERO),
                "a zero-length cooldown is always elapsed");
    }

    @Test
    void differentTickersAreIndependent() {
        assertTrue(coordinator.beginIfAbsent("NVDA"));
        assertTrue(coordinator.beginIfAbsent("AMD"), "different ticker is not blocked");
    }
}
