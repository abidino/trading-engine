package com.trading.discovery.infrastructure;

import com.trading.discovery.domain.model.DiscoveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaDiscoveryCandidateRepository extends JpaRepository<DiscoveryCandidateEntity, UUID> {

    Optional<DiscoveryCandidateEntity> findByTicker(String ticker);

    List<DiscoveryCandidateEntity> findByStatusOrderByEvaluatedAtDesc(DiscoveryStatus status);

    List<DiscoveryCandidateEntity> findAllByOrderByEvaluatedAtDesc();
}
