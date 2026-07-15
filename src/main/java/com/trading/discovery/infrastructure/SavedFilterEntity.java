package com.trading.discovery.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence for a saved screener filter. The criteria are stored as a single JSON
 * document ({@code selectionsJson} = filterKey -> Finviz token map) mirroring the
 * Finviz single-select model, plus the advanced raw-token passthrough.
 */
@Entity
@Table(name = "discovery_filters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedFilterEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private boolean active;

    /** JSON object: filterKey -> Finviz option token. */
    @Column(columnDefinition = "text")
    private String selectionsJson;

    @Column(columnDefinition = "text")
    private String rawFinvizFilters;

    @Column(nullable = false)
    private Instant createdAt;
}
