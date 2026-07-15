package com.trading.discovery.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaSavedFilterRepository extends JpaRepository<SavedFilterEntity, UUID> {

    List<SavedFilterEntity> findByActiveTrue();

    List<SavedFilterEntity> findAllByOrderByCreatedAtDesc();
}
