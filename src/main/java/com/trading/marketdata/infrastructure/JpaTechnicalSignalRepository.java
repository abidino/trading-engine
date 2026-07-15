package com.trading.marketdata.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaTechnicalSignalRepository extends JpaRepository<TechnicalSignalEntity, UUID> {
    List<TechnicalSignalEntity> findByTicker(String ticker);
}
