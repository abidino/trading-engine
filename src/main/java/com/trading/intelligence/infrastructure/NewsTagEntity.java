package com.trading.intelligence.infrastructure;

import com.trading.intelligence.domain.model.NewsSentiment;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "news_tags",
        indexes = {
                @Index(name = "idx_news_tags_ticker", columnList = "ticker")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsTagEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private NewsArticleEntity article;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NewsSentiment sentiment;

    @Column(columnDefinition = "TEXT")
    private String interpretation;
}
