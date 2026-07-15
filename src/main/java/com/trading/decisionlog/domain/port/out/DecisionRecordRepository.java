package com.trading.decisionlog.domain.port.out;

import com.trading.decisionlog.domain.model.DecisionRecord;
import com.trading.shared.kernel.Ticker;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port: decision-log persistence. */
public interface DecisionRecordRepository {
    DecisionRecord save(DecisionRecord record);
    Optional<DecisionRecord> findById(UUID id);
    List<DecisionRecord> findAll();
    List<DecisionRecord> findByTicker(Ticker ticker);
    Optional<DecisionRecord> findLatestByTicker(Ticker ticker);
    List<DecisionRecord> findPending();
}
