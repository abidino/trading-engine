package com.trading.portfolio.infrastructure;

import com.trading.portfolio.domain.model.PortfolioPosition;
import com.trading.portfolio.domain.port.out.PortfolioPositionRepository;
import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the domain port using Spring Data JPA.
 * Maps between domain model (PortfolioPosition) and JPA entity (PortfolioPositionEntity).
 */
@Component
@RequiredArgsConstructor
public class PortfolioPositionRepositoryAdapter implements PortfolioPositionRepository {

    private final JpaPortfolioPositionRepository jpa;

    @Override
    public List<PortfolioPosition> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<PortfolioPosition> findAllActive() {
        return jpa.findByActiveTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<PortfolioPosition> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PortfolioPosition> findByTicker(Ticker ticker) {
        return jpa.findByTicker(ticker.value()).map(this::toDomain);
    }

    @Override
    public PortfolioPosition save(PortfolioPosition position) {
        return toDomain(jpa.save(toEntity(position)));
    }

    // -----------------------------------------------------------------------
    // Mapping
    // -----------------------------------------------------------------------

    private PortfolioPosition toDomain(PortfolioPositionEntity e) {
        PortfolioPosition pos = new PortfolioPosition(
                e.getId(), new Ticker(e.getTicker()),
                e.getQuantity(), Money.of(e.getAverageEntryPrice()));
        if (e.getStopLossLevel() != null) {
            pos.updateStopLoss(Money.of(e.getStopLossLevel()));
        }
        return pos;
    }

    private PortfolioPositionEntity toEntity(PortfolioPosition p) {
        return PortfolioPositionEntity.builder()
                .id(p.getId())
                .ticker(p.getTicker().value())
                .quantity(p.getQuantity())
                .averageEntryPrice(p.getAverageEntryPrice().amount())
                .stopLossLevel(p.getStopLossLevel() != null ? p.getStopLossLevel().amount() : null)
                .active(p.isActive())
                .build();
    }
}
