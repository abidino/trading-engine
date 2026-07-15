package com.trading.discovery.infrastructure;

import com.trading.discovery.domain.model.FilterCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Loads the scraped Finviz filter catalog ({@code finviz/filter-catalog.json}) once at
 * startup and serves it as the single source of truth for both the UI dropdowns and the
 * {@link FinvizQueryMapper}. Because the file is generated verbatim from Finviz's own
 * option lists, every offered token is guaranteed to be one Finviz actually honours.
 */
@Slf4j
@Component
public class FinvizFilterCatalog {

    private final FilterCatalog catalog;
    private final Map<String, String> prefixByKey;
    private final Map<String, Set<String>> tokensByKey;

    public FinvizFilterCatalog(ObjectMapper objectMapper) {
        this.catalog = load(objectMapper);
        this.prefixByKey = catalog.prefixByKey();
        this.tokensByKey = catalog.tokensByKey();
        log.info("Loaded Finviz filter catalog: {} filters",
                catalog.groups().stream().mapToInt(g -> g.filters().size()).sum());
    }

    private FilterCatalog load(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("finviz/filter-catalog.json").getInputStream()) {
            return objectMapper.readValue(in, FilterCatalog.class);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load finviz/filter-catalog.json", e);
        }
    }

    public FilterCatalog catalog() {
        return catalog;
    }

    /** Finviz token prefix for a filter key, or {@code null} if unknown. */
    public String prefixFor(String key) {
        return prefixByKey.get(key);
    }

    /** True when {@code token} is a real Finviz option for {@code key}. */
    public boolean isValid(String key, String token) {
        Set<String> tokens = tokensByKey.get(key);
        return tokens != null && tokens.contains(token);
    }
}
