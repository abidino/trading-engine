package com.trading.discovery.web;

import com.trading.discovery.domain.model.DiscoveryCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record DiscoveredStockResponse(
        String ticker, String companyName, String sector,
        double marketCap, Double peRatio, double score,
        String status, boolean recommended, Double confidence,
        String reasoning, String trendDirection,
        List<String> signals, String discoveredAt, String evaluatedAt
) {
    public static DiscoveredStockResponse from(DiscoveryCandidate c) {
        Map<String, String> criteria = c.matchedCriteria();
        return new DiscoveredStockResponse(
                c.ticker(),
                c.companyName(),
                c.sector(),
                parseDouble(criteria.get("marketCap")),
                parseDoubleOrNull(criteria.get("peRatio")),
                c.confidence() != null ? c.confidence() : 0.0,
                c.status().name(),
                c.recommended(),
                c.confidence(),
                c.reasoning(),
                c.trendDirection(),
                new ArrayList<>(criteria.keySet()),
                c.discoveredAt() != null ? c.discoveredAt().toString() : null,
                c.evaluatedAt() != null ? c.evaluatedAt().toString() : null
        );
    }

    private static double parseDouble(String v) {
        try { return v != null ? Double.parseDouble(stripUnits(v)) : 0; }
        catch (NumberFormatException e) { return 0; }
    }

    private static Double parseDoubleOrNull(String v) {
        try { return v != null ? Double.parseDouble(stripUnits(v)) : null; }
        catch (NumberFormatException e) { return null; }
    }

    /** Finviz reports values like "12.3B" or "-" — keep only the numeric part. */
    private static String stripUnits(String v) {
        return v.replaceAll("[^0-9.\\-]", "");
    }
}
