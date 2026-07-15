package com.trading.intelligence.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaSocialSignalRepository extends JpaRepository<SocialSignalEntity, UUID> {
    List<SocialSignalEntity> findByTickerOrderByCapturedAtDesc(String ticker);
}
