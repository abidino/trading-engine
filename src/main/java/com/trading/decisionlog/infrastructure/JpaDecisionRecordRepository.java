package com.trading.decisionlog.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaDecisionRecordRepository extends JpaRepository<DecisionRecordEntity, UUID> {
    List<DecisionRecordEntity> findByTickerOrderByDecidedAtDesc(String ticker);
    List<DecisionRecordEntity> findByOutcome(String outcome);
    java.util.Optional<DecisionRecordEntity> findFirstByTickerOrderByDecidedAtDesc(String ticker);
}
