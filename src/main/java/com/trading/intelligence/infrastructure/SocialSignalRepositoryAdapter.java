package com.trading.intelligence.infrastructure;

import com.trading.intelligence.domain.model.SocialSignal;
import com.trading.intelligence.domain.port.out.SocialSignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SocialSignalRepositoryAdapter implements SocialSignalRepository {

    private final JpaSocialSignalRepository jpa;

    @Override
    public List<SocialSignal> findByTicker(String ticker) {
        return jpa.findByTickerOrderByCapturedAtDesc(ticker).stream().map(this::toDomain).toList();
    }

    @Override
    public void saveAll(List<SocialSignal> signals) {
        jpa.saveAll(signals.stream().map(this::toEntity).toList());
    }

    private SocialSignal toDomain(SocialSignalEntity e) {
        return new SocialSignal(e.getId(), e.getTicker(), e.getSource(), e.getContent(),
                e.getEngagementScore().doubleValue(),
                e.getSentimentScore() != null ? e.getSentimentScore().doubleValue() : 0.0,
                e.getCapturedAt());
    }

    private SocialSignalEntity toEntity(SocialSignal s) {
        return SocialSignalEntity.builder()
                .id(s.id()).ticker(s.ticker()).source(s.source()).content(s.content())
                .engagementScore(BigDecimal.valueOf(s.engagementScore()))
                .sentimentScore(BigDecimal.valueOf(s.sentimentScore()))
                .capturedAt(s.capturedAt())
                .build();
    }
}
