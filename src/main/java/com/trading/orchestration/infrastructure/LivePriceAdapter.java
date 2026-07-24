package com.trading.orchestration.infrastructure;

import com.trading.marketdata.domain.model.IntradayQuote;
import com.trading.marketdata.domain.port.in.IntradayQuoteUseCase;
import com.trading.orchestration.domain.port.out.LivePricePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Anticorruption adapter: exposes the marketdata module's live intraday quote
 * (pre-market / regular / post-market) to the orchestration domain as a compact
 * {@link Snapshot}, degrading gracefully to {@code null} when unavailable.
 *
 * <p>{@link #ensureFresh(String)} pulls the newest quote straight from Yahoo (including
 * pre/post-market prints) so the analysis always evaluates levels against the true latest price.</p>
 */
@Slf4j
@Component
public class LivePriceAdapter implements LivePricePort {

    private final IntradayQuoteUseCase intradayQuotes;

    public LivePriceAdapter(IntradayQuoteUseCase intradayQuotes) {
        this.intradayQuotes = intradayQuotes;
    }

    @Override
    public void ensureFresh(String ticker) {
        try {
            intradayQuotes.refreshQuote(ticker);
        } catch (Exception e) {
            log.warn("Live price refresh failed for {}: {}", ticker, e.getMessage());
        }
    }

    @Override
    public Snapshot fetchForTicker(String ticker) {
        try {
            Optional<IntradayQuote> latest = intradayQuotes.latestQuote(ticker);
            if (latest.isEmpty() || latest.get().price() == null) {
                return null;
            }
            IntradayQuote q = latest.get();
            return new Snapshot(
                    q.price().doubleValue(),
                    q.session() != null ? q.session().name() : null,
                    q.previousClose() != null ? q.previousClose().doubleValue() : null,
                    q.changePercent(),
                    q.quoteTime() != null ? q.quoteTime().toString() : null);
        } catch (Exception e) {
            log.warn("Live price unavailable for {}: {}", ticker, e.getMessage());
            return null;
        }
    }
}
