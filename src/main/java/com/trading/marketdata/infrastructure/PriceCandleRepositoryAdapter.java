package com.trading.marketdata.infrastructure;

import com.trading.marketdata.domain.model.PriceCandle;
import com.trading.marketdata.domain.port.out.PriceCandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class PriceCandleRepositoryAdapter implements PriceCandleRepository {

    private final JpaPriceCandleRepository jpa;
    private final JpaTechnicalSignalRepository signalJpa;

    @Override
    public List<PriceCandle> findByTicker(String ticker) {
        return jpa.findByTicker(ticker).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PriceCandle> findByTickerAndDateBetween(String ticker, LocalDate from, LocalDate to) {
        return jpa.findByTickerAndCandleDateBetween(ticker, from, to).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<PriceCandle> findByTickerAndDate(String ticker, LocalDate date) {
        return jpa.findByTickerAndCandleDate(ticker, date).map(this::toDomain);
    }


    @Override
    public List<PriceCandle> findTickerByLastDay(Collection<String> ticker) {
        return jpa.findLatestCandleForEachTicker(ticker)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PriceCandle save(PriceCandle candle) {
        return toDomain(jpa.save(toEntity(candle)));
    }

    @Override
    public void saveAll(List<PriceCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        Map<String, UUID> existingIdByKey = candles.stream()
                .map(PriceCandle::ticker)
                .distinct()
                .flatMap(ticker -> jpa.findByTicker(ticker).stream())
                .collect(Collectors.toMap(
                        e -> key(e.getTicker(), e.getCandleDate()),
                        PriceCandleEntity::getId,
                        (a, b) -> a));

        List<PriceCandleEntity> entities = candles.stream()
                .map(c -> {
                    PriceCandleEntity e = toEntity(c);
                    UUID existingId = existingIdByKey.get(key(c.ticker(), c.candleDate()));
                    if (existingId != null) {
                        e.setId(existingId);
                    }
                    return e;
                })
                .toList();

        jpa.saveAll(entities);
    }

    private String key(String ticker, LocalDate date) {
        return ticker + "|" + date;
    }

    private PriceCandle toDomain(PriceCandleEntity e) {
        return new PriceCandle(e.getId(), e.getTicker(), e.getCandleDate(),
                e.getOpen(), e.getHigh(), e.getLow(), e.getClose(), e.getVolume());
    }

    private PriceCandleEntity toEntity(PriceCandle c) {
        return PriceCandleEntity.builder()
                .id(c.id()).ticker(c.ticker()).candleDate(c.candleDate())
                .open(c.open()).high(c.high()).low(c.low()).close(c.close()).volume(c.volume())
                .build();
    }
}
