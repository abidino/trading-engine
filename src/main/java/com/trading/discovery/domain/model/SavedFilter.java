package com.trading.discovery.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate: a named, persisted screener filter configuration that the user can
 * save from the UI, activate/deactivate, and re-use across discovery cycles.
 */
public record SavedFilter(
        UUID id,
        String name,
        String description,
        boolean active,
        DiscoveryFilter criteria,
        Instant createdAt
) {
    public static SavedFilter create(String name, String description, DiscoveryFilter criteria) {
        return new SavedFilter(UUID.randomUUID(), name, description, true, criteria, Instant.now());
    }

    public SavedFilter withActive(boolean newActive) {
        return new SavedFilter(id, name, description, newActive, criteria, createdAt);
    }
}
