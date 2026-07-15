package com.trading.orchestration.infrastructure;

import com.trading.intelligence.domain.IntelligenceApplicationService;
import com.trading.intelligence.domain.model.SocialSignal;
import com.trading.intelligence.domain.port.out.SocialSignalRepository;
import com.trading.orchestration.domain.port.out.SocialSignalPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Reads social signals from the intelligence module's persisted records,
 * gathering fresh ones on demand when the local cache is missing or stale.
 */
@Slf4j
@Component
public class SocialIntelligenceAdapter implements SocialSignalPort {

    private final SocialSignalRepository socialSignalRepository;
    private final IntelligenceApplicationService intelligenceService;
    private final Duration freshness;
    private final int collectLimit;

    public SocialIntelligenceAdapter(
            SocialSignalRepository socialSignalRepository,
            IntelligenceApplicationService intelligenceService,
            @Value("${orchestration.freshness.social-minutes:30}") long freshnessMinutes,
            @Value("${orchestration.freshness.social-collect-limit:20}") int collectLimit) {
        this.socialSignalRepository = socialSignalRepository;
        this.intelligenceService = intelligenceService;
        this.freshness = Duration.ofMinutes(freshnessMinutes);
        this.collectLimit = collectLimit;
    }

    @Override
    public void ensureFresh(String ticker) {
        Instant newest = socialSignalRepository.findByTicker(ticker).stream()
                .map(SocialSignal::capturedAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        if (newest != null && newest.isAfter(Instant.now().minus(freshness))) {
            log.debug("Social for {} fresh (captured {}), skipping collect", ticker, newest);
            return;
        }
        try {
            intelligenceService.collectSocialSignalsForTicker(ticker, collectLimit);
            log.info("Social readiness: collected signals for {}", ticker);
        } catch (Exception e) {
            log.warn("Social readiness collect failed for {}: {}", ticker, e.getMessage());
        }
    }

    @Override
    public List<String> fetchSignalsForTicker(String ticker, int limit) {
        return socialSignalRepository.findByTicker(ticker).stream()
                .limit(limit)
                .map(s -> "[%s | engagement=%.2f] %s"
                        .formatted(s.source(), s.engagementScore(), s.content()))
                .toList();
    }
}
