package com.trading.discovery.web;

import com.trading.discovery.domain.model.DiscoveryFilter;

import java.util.Map;

/**
 * Request body for creating a saved filter or running an ad-hoc screen.
 * {@code selections} maps each filter key to a single Finviz option token
 * (mirroring the free screener's single-select model).
 */
public record CreateFilterRequest(
        String name,
        String description,
        Map<String, String> selections,
        String rawFinvizFilters
) {
    public DiscoveryFilter toDiscoveryFilter() {
        return new DiscoveryFilter(selections != null ? selections : Map.of(), rawFinvizFilters);
    }
}
