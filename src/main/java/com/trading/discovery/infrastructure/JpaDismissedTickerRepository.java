package com.trading.discovery.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaDismissedTickerRepository extends JpaRepository<DismissedTickerEntity, UUID> {

    Optional<DismissedTickerEntity> findByTicker(String ticker);

    boolean existsByTicker(String ticker);

    void deleteByTicker(String ticker);
}
