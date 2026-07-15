package com.trading.discovery.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Value Object: the user's screener selection — a faithful mirror of the Finviz free
 * screener, which honours exactly <b>one token per filter family</b> (no ranges, no
 * multi-select). Each entry is {@code filterKey -> Finviz option token} (the token as it
 * appears in Finviz's dropdown, WITHOUT the family prefix — the prefix is resolved from
 * {@link FilterCatalog}). Example: {@code {"pe":"u20","sector":"technology","country":"usa"}}.
 *
 * <p>{@code rawFinvizFilters} is an advanced passthrough — extra Finviz tokens
 * (comma-separated, e.g. {@code "sh_avgvol_o500"}) appended verbatim.</p>
 */
public record DiscoveryFilter(
        Map<String, String> selections,
        String rawFinvizFilters
) {
    public DiscoveryFilter {
        Map<String, String> clean = new LinkedHashMap<>();
        if (selections != null) {
            selections.forEach((k, v) -> {
                if (k != null && v != null && !k.isBlank() && !v.isBlank()) {
                    clean.put(k, v);
                }
            });
        }
        selections = Map.copyOf(clean);
    }

    /** Selected Finviz token for a filter key, or {@code null} if not set. */
    public String token(String key) {
        return selections.get(key);
    }

    public boolean isEmpty() {
        return selections.isEmpty() && (rawFinvizFilters == null || rawFinvizFilters.isBlank());
    }

    /**
     * Sensible defaults for a growth-oriented, financially-healthy discovery run.
     * Uses only verified Finviz tokens.
     */
    public static DiscoveryFilter defaults() {
        Map<String, String> s = new LinkedHashMap<>();
        s.put("country", "usa");
        s.put("marketCap", "smallover");   // > $300M (skip nano/micro noise)
        s.put("price", "o5");              // price > $5
        s.put("pe", "profitable");         // P/E > 0 (profitable)
        s.put("peg", "u2");                // PEG < 2 (growth at reasonable price)
        s.put("relVol", "o1.5");           // relative volume > 1.5x
        s.put("roe", "o10");               // ROE > +10%
        s.put("debtEq", "u1");             // debt/equity < 1
        s.put("currentRatio", "o1.5");     // current ratio > 1.5
        s.put("targetPrice", "a5");        // >= 5% analyst upside
        return new DiscoveryFilter(s, null);
    }
}
