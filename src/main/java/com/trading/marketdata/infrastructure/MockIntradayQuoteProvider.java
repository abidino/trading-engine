package com.trading.marketdata.infrastructure;

import com.trading.marketdata.domain.model.IntradayQuote;
import com.trading.marketdata.domain.model.MarketSession;
import com.trading.marketdata.domain.port.out.IntradayQuoteProviderPort;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/** Deterministic {@link IntradayQuoteProviderPort} stub for the {@code mock} profile. */
@Primary
@Profile("mock")
@Component
public class MockIntradayQuoteProvider implements IntradayQuoteProviderPort {

    @Override
    public Optional<IntradayQuote> fetchQuote(String ticker) {
        // Deterministic-ish price seeded by the ticker so different symbols differ.
        double previousClose = 100.0 + (ticker.hashCode() % 50 + 50) % 50;
        double price = previousClose + 1.25;
        return Optional.of(IntradayQuote.create(
                ticker,
                MarketSession.REGULAR,
                BigDecimal.valueOf(price),
                BigDecimal.valueOf(previousClose),
                1_500_000L,
                Instant.now()));
    }
}
