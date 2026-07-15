package com.trading.marketdata.domain;

import com.trading.marketdata.domain.model.IntradayQuote;
import com.trading.marketdata.domain.port.in.IntradayQuoteUseCase;
import com.trading.marketdata.domain.port.out.IntradayQuoteProviderPort;
import com.trading.marketdata.domain.port.out.IntradayQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Application service: fetches and persists intraday quotes, kept separately from
 * the daily OHLC candles. Designed to be polled every ~10 minutes by a job, but
 * each ticker can also be refreshed on demand via the API.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class IntradayQuoteApplicationService implements IntradayQuoteUseCase {

    private final IntradayQuoteProviderPort quoteProvider;
    private final IntradayQuoteRepository quoteRepository;

    @Override
    public IntradayQuote refreshQuote(String ticker) {
        String symbol = ticker.toUpperCase();
        IntradayQuote quote = quoteProvider.fetchQuote(symbol)
                .orElseThrow(() -> new IllegalStateException("No intraday quote available for " + symbol));
        IntradayQuote saved = quoteRepository.save(quote);
        log.debug("Refreshed intraday quote {} = {} ({})", symbol, saved.price(), saved.session());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IntradayQuote> latestQuote(String ticker) {
        return quoteRepository.findLatestByTicker(ticker.toUpperCase());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntradayQuote> quoteHistory(String ticker, int sinceMinutes) {
        Instant since = Instant.now().minus(Math.max(1, sinceMinutes), ChronoUnit.MINUTES);
        return quoteRepository.findByTickerSince(ticker.toUpperCase(), since);
    }
}
