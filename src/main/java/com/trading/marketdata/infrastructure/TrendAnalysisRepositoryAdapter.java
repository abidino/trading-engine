package com.trading.marketdata.infrastructure;

import com.trading.marketdata.domain.model.TechnicalIndicatorSnapshot;
import com.trading.marketdata.domain.model.TrendAnalysis;
import com.trading.marketdata.domain.port.out.TrendAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TrendAnalysisRepositoryAdapter implements TrendAnalysisRepository {

    private final JpaTrendAnalysisRepository jpa;

    @Override
    public TrendAnalysis save(TrendAnalysis analysis) {
        return toDomain(jpa.save(toEntity(analysis)));
    }

    @Override
    public Optional<TrendAnalysis> findLatestByTicker(String ticker) {
        return jpa.findFirstByTickerOrderByAnalysisDateDesc(ticker).map(this::toDomain);
    }

    @Override
    public List<TrendAnalysis> findRecentByTicker(String ticker, int limit) {
        return jpa.findByTickerOrderByAnalysisDateDesc(ticker, PageRequest.of(0, Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<TrendAnalysis> findByTickerAndDateBetween(String ticker, LocalDate from, LocalDate to) {
        return jpa.findByTickerAndAnalysisDateBetweenOrderByAnalysisDateDesc(ticker, from, to)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<TrendAnalysis> findByTickerAndDate(String ticker, LocalDate analysisDate) {
        return jpa.findByTickerAndAnalysisDate(ticker, analysisDate).map(this::toDomain);
    }

    // -----------------------------------------------------------------------

    private TrendAnalysis toDomain(TrendAnalysisEntity e) {
        TechnicalIndicatorSnapshot snapshot = new TechnicalIndicatorSnapshot(
                e.getTicker(), e.getAnalysisDate(), e.getClosePrice(),
                e.getEma9(), e.getEma20(), e.getEma50(), e.getEma100(), e.getEma200(),
                e.getRsi14(), e.getMacd(), e.getMacdSignal(), e.getMacdHistogram(),
                e.getDataPoints());
        return new TrendAnalysis(
                e.getId(), e.getTicker(), e.getAnalysisDate(), e.getTrend(),
                e.getConfidence(), e.getReasoning(), snapshot, e.getLlmModel(), e.getCreatedAt());
    }

    private TrendAnalysisEntity toEntity(TrendAnalysis t) {
        TechnicalIndicatorSnapshot s = t.snapshot();
        return TrendAnalysisEntity.builder()
                .id(t.id())
                .ticker(t.ticker())
                .analysisDate(t.analysisDate())
                .trend(t.trend())
                .confidence(t.confidence())
                .reasoning(t.reasoning())
                .closePrice(s.close())
                .ema9(s.ema9()).ema20(s.ema20()).ema50(s.ema50())
                .ema100(s.ema100()).ema200(s.ema200())
                .rsi14(s.rsi14())
                .macd(s.macd()).macdSignal(s.macdSignal()).macdHistogram(s.macdHistogram())
                .dataPoints(s.dataPoints())
                .llmModel(t.llmModel())
                .createdAt(t.createdAt())
                .build();
    }
}
