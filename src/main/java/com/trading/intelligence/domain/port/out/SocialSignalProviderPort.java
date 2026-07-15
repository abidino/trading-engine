package com.trading.intelligence.domain.port.out;

import com.trading.intelligence.domain.model.SocialSignal;

import java.util.List;

/** Outbound port: external social media signal provider (Reddit, Twitter/X, etc.). */
public interface SocialSignalProviderPort {
    List<SocialSignal> fetchForTicker(String ticker, int limit);
}
