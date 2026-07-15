package com.trading.discovery.infrastructure;

import com.trading.discovery.domain.model.DiscoveryFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Maps a domain {@link DiscoveryFilter} to the Finviz screener {@code f=} query
 * parameter (comma-separated token list).
 *
 * <p>Because {@link DiscoveryFilter} already stores the exact Finviz option token per
 * filter family (single-select, mirroring the free screener), this mapper is a trivial
 * {@code prefix + token} join. Prefixes come from the {@link FinvizFilterCatalog}
 * (single source of truth), so adding a filter means only editing the catalog JSON.
 * Any selection whose key/token is not in the catalog is skipped and logged, so no
 * silently-ignored fake filters can reach Finviz.</p>
 */
@Slf4j
@Component
public class FinvizQueryMapper {

    private final FinvizFilterCatalog catalog;

    public FinvizQueryMapper(FinvizFilterCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Builds the Finviz {@code f=} value (comma-separated tokens) for the given filter.
     * Unknown/invalid selections are omitted. Returns an empty string when nothing is set.
     */
    public String toFilterParam(DiscoveryFilter f) {
        StringJoiner sj = new StringJoiner(",");

        for (Map.Entry<String, String> e : f.selections().entrySet()) {
            String key = e.getKey();
            String token = e.getValue();
            String prefix = catalog.prefixFor(key);
            if (prefix == null) {
                log.warn("Finviz mapper: unknown filter key '{}' — skipping", key);
                continue;
            }
            if (!catalog.isValid(key, token)) {
                log.warn("Finviz mapper: token '{}' is not a valid Finviz option for '{}' — skipping",
                        token, key);
                continue;
            }
            sj.add(prefix + token);
        }

        if (f.rawFinvizFilters() != null && !f.rawFinvizFilters().isBlank()) {
            for (String token : f.rawFinvizFilters().split(",")) {
                if (!token.isBlank()) {
                    sj.add(token.trim());
                }
            }
        }

        return sj.toString();
    }
}
