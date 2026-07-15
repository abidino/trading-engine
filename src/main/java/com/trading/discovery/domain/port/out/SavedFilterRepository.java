package com.trading.discovery.domain.port.out;

import com.trading.discovery.domain.model.SavedFilter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port: persistence for named, saved screener filters. */
public interface SavedFilterRepository {

    SavedFilter save(SavedFilter filter);

    List<SavedFilter> findAll();

    List<SavedFilter> findAllActive();

    Optional<SavedFilter> findById(UUID id);

    void deleteById(UUID id);
}
