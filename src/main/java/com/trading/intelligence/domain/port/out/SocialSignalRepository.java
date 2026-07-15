package com.trading.intelligence.domain.port.out;

import com.trading.intelligence.domain.model.SocialSignal;

import java.util.List;

/** Outbound port: local social signal cache persistence. */
public interface SocialSignalRepository {
    List<SocialSignal> findByTicker(String ticker);
    void saveAll(List<SocialSignal> signals);
}
