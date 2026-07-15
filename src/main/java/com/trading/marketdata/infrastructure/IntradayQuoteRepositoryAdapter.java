package com.trading.marketdata.infrastructure;

import com.trading.marketdata.domain.model.IntradayQuote;
import com.trading.marketdata.domain.port.out.IntradayQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IntradayQuoteRepositoryAdapter implements IntradayQuoteRepository {

    private final JpaIntradayQuoteRepository jpa;

    @Override
    public IntradayQuote save(IntradayQuote quote) {
        return toDomain(jpa.save(toEntity(quote)));
    }

    @Override
    public Optional<IntradayQuote> findLatestByTicker(String ticker) {
        return jpa.findFirstByTickerOrderByCapturedAtDesc(ticker).map(this::toDomain);
    }

    @Override
    public List<IntradayQuote> findByTickerSince(String ticker, Instant since) {
        return jpa.findByTickerAndCapturedAtGreaterThanEqualOrderByCapturedAtDesc(ticker, since)
                .stream().map(this::toDomain).toList();
    }

    private IntradayQuoteEntity toEntity(IntradayQuote q) {
        return IntradayQuoteEntity.builder()
                .id(q.id())
                .ticker(q.ticker())
                .session(q.session())
                .price(q.price())
                .previousClose(q.previousClose())
                .change(q.change())
                .changePercent(q.changePercent())
                .volume(q.volume())
                .quoteTime(q.quoteTime())
                .capturedAt(q.capturedAt())
                .build();
    }

    private IntradayQuote toDomain(IntradayQuoteEntity e) {
        return new IntradayQuote(
                e.getId(), e.getTicker(), e.getSession(), e.getPrice(), e.getPreviousClose(),
                e.getChange(), e.getChangePercent(), e.getVolume(), e.getQuoteTime(), e.getCapturedAt());
    }
}
