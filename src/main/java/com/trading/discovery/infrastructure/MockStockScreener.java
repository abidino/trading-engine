package com.trading.discovery.infrastructure;

import com.trading.discovery.domain.model.DiscoveryFilter;
import com.trading.discovery.domain.model.PotentialStock;
import com.trading.discovery.domain.port.out.StockScreenerPort;
import com.trading.shared.kernel.Ticker;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Deterministic {@link StockScreenerPort} stub for the {@code mock} profile. */
@Primary
@Profile("mock")
@Component
public class MockStockScreener implements StockScreenerPort {

    @Override
    public List<PotentialStock> screen(DiscoveryFilter filter) {
        List<PotentialStock> out = new ArrayList<>();
        String[] tickers = {"AAPL", "MSFT", "NVDA", "AMD", "GOOGL"};
        for (String t : tickers) {
            out.add(PotentialStock.create(
                    new Ticker(t),
                    "mock-screener",
                    Map.of("source", "mock", "matched", "true")));
        }
        return out;
    }
}
