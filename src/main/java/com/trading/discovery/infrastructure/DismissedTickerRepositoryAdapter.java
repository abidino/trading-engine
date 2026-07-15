package com.trading.discovery.infrastructure;

import com.trading.discovery.domain.port.out.DismissedTickerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DismissedTickerRepositoryAdapter implements DismissedTickerRepository {

    /** How long a ticker stays suppressed (no re-analysis, hidden from UI) before it can return. */
    static final int SUPPRESSION_DAYS = 15;

    static final String TYPE_DISMISSED = "DISMISSED";
    static final String TYPE_AUTO = "AUTO";

    private final JpaDismissedTickerRepository jpa;

    @Override
    public void dismiss(String ticker, String reason) {
        upsert(ticker, TYPE_DISMISSED, reason);
    }

    @Override
    public void suppress(String ticker, String reason) {
        upsert(ticker, TYPE_AUTO, reason);
    }

    @Override
    public boolean isSuppressed(String ticker) {
        return jpa.findByTicker(ticker).map(this::isActive).orElse(false);
    }

    @Override
    public boolean isDismissed(String ticker) {
        return jpa.findByTicker(ticker)
                .map(e -> TYPE_DISMISSED.equals(e.getSuppressionType()) && isActive(e))
                .orElse(false);
    }

    @Override
    public List<String> findAllTickers() {
        return jpa.findAll().stream()
                .filter(e -> TYPE_DISMISSED.equals(e.getSuppressionType()) && isActive(e))
                .map(DismissedTickerEntity::getTicker)
                .toList();
    }

    @Override
    @Transactional
    public void remove(String ticker) {
        jpa.deleteByTicker(ticker);
    }

    // -----------------------------------------------------------------------

    private void upsert(String ticker, String type, String reason) {
        Instant now = Instant.now();
        DismissedTickerEntity e = jpa.findByTicker(ticker).orElseGet(() -> {
            DismissedTickerEntity n = new DismissedTickerEntity();
            n.setId(UUID.randomUUID());
            n.setTicker(ticker);
            return n;
        });
        e.setSuppressionType(type);
        e.setReason(reason);
        e.setDismissedAt(now);
        e.setSuppressedUntil(now.plus(SUPPRESSION_DAYS, ChronoUnit.DAYS));
        jpa.save(e);
    }

    private boolean isActive(DismissedTickerEntity e) {
        return e.getSuppressedUntil() == null || e.getSuppressedUntil().isAfter(Instant.now());
    }
}
