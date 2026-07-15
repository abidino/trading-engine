package com.trading.intelligence.infrastructure;

import com.trading.intelligence.domain.model.NewsCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "news_articles",
        uniqueConstraints = @UniqueConstraint(columnNames = "url")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsArticleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String headline;

    @Column(nullable = false, length = 120)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NewsCategory category;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant publishedAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant capturedAt;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<NewsTagEntity> tags = new ArrayList<>();
}
