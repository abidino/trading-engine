package com.trading.orchestration.infrastructure;

import com.trading.orchestration.domain.model.FundamentalData;
import com.trading.orchestration.domain.port.out.FundamentalDataPort;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Deterministic {@link FundamentalDataPort} stub for the {@code mock} profile. */
@Primary
@Profile("mock")
@Component
public class MockFundamentalDataProvider implements FundamentalDataPort {

    @Override
    public FundamentalData fetchForTicker(String ticker) {
        return new FundamentalData(
                ticker,
                24.5,          // peRatio
                6.10,          // eps
                1_200_000_000L,// marketCapUsd
                0.12,          // revenueGrowthRate
                0.45,          // debtToEquity
                "Technology",
                "Software",
                "Mock company operating in the technology sector for offline testing.");
    }
}
