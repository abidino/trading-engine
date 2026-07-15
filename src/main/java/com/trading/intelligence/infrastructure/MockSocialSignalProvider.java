package com.trading.intelligence.infrastructure;

import com.trading.intelligence.domain.model.SocialSignal;
import com.trading.intelligence.domain.port.out.SocialSignalProviderPort;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Deterministic {@link SocialSignalProviderPort} stub for the {@code mock} profile. */
@Primary
@Profile("mock")
@Component
public class MockSocialSignalProvider implements SocialSignalProviderPort {

    @Override
    public List<SocialSignal> fetchForTicker(String ticker, int limit) {
        List<SocialSignal> out = new ArrayList<>();
        String[] posts = {
                "Anyone else bullish on " + ticker + " after the latest news?",
                ticker + " looks like a solid long-term hold.",
                "Not sure about " + ticker + " at these levels, feels stretched."
        };
        int n = Math.min(limit, posts.length);
        for (int i = 0; i < n; i++) {
            out.add(SocialSignal.create(ticker, "r/stocks", posts[i], 2.5 - i * 0.3, 0.0));
        }
        return out;
    }
}
