package com.trading.discovery.web;

import com.trading.discovery.domain.model.DiscoveryFilter;
import com.trading.discovery.domain.model.SavedFilter;

import java.util.Map;

public record FilterRecord(
        String id, String name, String description, boolean active,
        Map<String, String> selections, String rawFinvizFilters, String createdAt
) {
    public static FilterRecord from(SavedFilter f) {
        DiscoveryFilter c = f.criteria();
        return new FilterRecord(
                f.id().toString(), f.name(), f.description(), f.active(),
                c.selections(), c.rawFinvizFilters(),
                f.createdAt() != null ? f.createdAt().toString() : null
        );
    }
}
