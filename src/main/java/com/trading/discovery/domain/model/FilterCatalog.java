package com.trading.discovery.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Faithful, data-driven mirror of the Finviz <b>free</b> screener filter dropdowns.
 *
 * <p>The Finviz free screener honours exactly <b>one token per filter family</b>
 * (verified live: a second same-family token or a second sector is silently dropped —
 * the last one wins, there are no ranges and no multi-select). Therefore every filter
 * here is modelled as a single-select list of the <i>exact</i> option tokens Finviz
 * itself offers, scraped from its dropdowns. Sending {@code prefix + token} reproduces
 * Finviz's own result set 1:1.</p>
 */
public record FilterCatalog(List<Group> groups) {

    public record Group(String group, List<Filter> filters) {}

    public record Filter(String key, String label, String group, String prefix, List<Option> options) {}

    public record Option(String token, String label) {}

    /** Flat index: filter key → Finviz token prefix (e.g. {@code "pe" → "fa_pe_"}). */
    public Map<String, String> prefixByKey() {
        return groups.stream()
                .flatMap(g -> g.filters().stream())
                .collect(java.util.stream.Collectors.toMap(Filter::key, Filter::prefix));
    }

    /** Flat index: filter key → set of valid tokens (for validation). */
    public Map<String, java.util.Set<String>> tokensByKey() {
        return groups.stream()
                .flatMap(g -> g.filters().stream())
                .collect(java.util.stream.Collectors.toMap(
                        Filter::key,
                        f -> f.options().stream().map(Option::token)
                                .collect(java.util.stream.Collectors.toSet())));
    }
}
