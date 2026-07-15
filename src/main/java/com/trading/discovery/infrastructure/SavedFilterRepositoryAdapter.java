package com.trading.discovery.infrastructure;

import com.trading.discovery.domain.model.DiscoveryFilter;
import com.trading.discovery.domain.model.SavedFilter;
import com.trading.discovery.domain.port.out.SavedFilterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class SavedFilterRepositoryAdapter implements SavedFilterRepository {

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final JpaSavedFilterRepository jpa;
    private final ObjectMapper objectMapper;

    public SavedFilterRepositoryAdapter(JpaSavedFilterRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public SavedFilter save(SavedFilter filter) {
        return toDomain(jpa.save(toEntity(filter)));
    }

    @Override
    public List<SavedFilter> findAll() {
        return jpa.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public List<SavedFilter> findAllActive() {
        return jpa.findByActiveTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<SavedFilter> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    // -----------------------------------------------------------------------

    private SavedFilterEntity toEntity(SavedFilter f) {
        DiscoveryFilter c = f.criteria();
        return SavedFilterEntity.builder()
                .id(f.id())
                .name(f.name())
                .description(f.description())
                .active(f.active())
                .selectionsJson(writeJson(c.selections()))
                .rawFinvizFilters(c.rawFinvizFilters())
                .createdAt(f.createdAt())
                .build();
    }

    private SavedFilter toDomain(SavedFilterEntity e) {
        DiscoveryFilter criteria = new DiscoveryFilter(readJson(e.getSelectionsJson()), e.getRawFinvizFilters());
        return new SavedFilter(e.getId(), e.getName(), e.getDescription(),
                e.isActive(), criteria, e.getCreatedAt());
    }

    private String writeJson(Map<String, String> selections) {
        if (selections == null || selections.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(selections);
    }

    private Map<String, String> readJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            log.warn("Could not parse saved filter selections '{}': {}", json, ex.getMessage());
            return Map.of();
        }
    }
}
