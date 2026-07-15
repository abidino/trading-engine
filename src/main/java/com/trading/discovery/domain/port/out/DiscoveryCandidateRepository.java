package com.trading.discovery.domain.port.out;

import com.trading.discovery.domain.model.DiscoveryCandidate;
import com.trading.discovery.domain.model.DiscoveryStatus;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: persistence for evaluated discovery candidates.
 * {@link #save} has upsert-by-ticker semantics (one current row per ticker).
 */
public interface DiscoveryCandidateRepository {

    DiscoveryCandidate save(DiscoveryCandidate candidate);

    Optional<DiscoveryCandidate> findByTicker(String ticker);

    List<DiscoveryCandidate> findByStatus(DiscoveryStatus status);

    /** Currently recommended candidates, newest evaluation first. */
    List<DiscoveryCandidate> findRecommended();
}
