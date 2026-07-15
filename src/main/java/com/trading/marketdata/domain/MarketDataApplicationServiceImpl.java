package com.trading.marketdata.domain;

import com.trading.marketdata.domain.model.PriceCandle;
import com.trading.marketdata.domain.model.TechnicalSignal;
import com.trading.marketdata.domain.port.in.MarketDataApplicationService;
import com.trading.marketdata.domain.port.out.IntradayQuoteRepository;
import com.trading.marketdata.domain.port.out.MarketDataProviderPort;
import com.trading.marketdata.domain.port.out.PriceCandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fetches market data from the external provider, stores it locally as a cache,
 * and serves it to the ai-orchestration module via infrastructure adapters.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MarketDataApplicationServiceImpl implements MarketDataApplicationService {

    private final MarketDataProviderPort providerPort;
    private final PriceCandleRepository candleRepository;
    private final IntradayQuoteRepository intradayQuoteRepository;

    public List<PriceCandle> syncAndGetCandles(String ticker, int lookbackDays) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(lookbackDays);

        List<PriceCandle> fresh = providerPort.fetchCandles(ticker, from, to);
        candleRepository.saveAll(fresh);
        log.debug("Synced {} candles for {}", fresh.size(), ticker);
        return fresh;
    }

    public List<TechnicalSignal> syncAndGetIndicators(String ticker, int lookbackDays) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(lookbackDays);
        return providerPort.fetchIndicators(ticker, from, to);
    }

    @Transactional(readOnly = true)
    public List<PriceCandle> getCachedCandles(String ticker) {
        return candleRepository.findByTicker(ticker);
    }

    @Override
    public Map<String, BigDecimal> getLatestPriceForTickers(List<String> tickers) {
        Map<String, BigDecimal> prices = new HashMap<>();
        // Baseline: last daily candle close.
        candleRepository.findTickerByLastDay(tickers)
                .forEach(c -> prices.put(c.ticker(), c.close()));
        // Prefer the fresher intraday quote when available (refreshed by the 5-min job
        // and on-demand on page load).
        for (String ticker : tickers) {
            intradayQuoteRepository.findLatestByTicker(ticker.toUpperCase())
                    .map(q -> q.price())
                    .filter(price -> price != null && price.signum() > 0)
                    .ifPresent(price -> prices.put(ticker, price));
        }
        return prices;
    }
}
