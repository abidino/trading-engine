package com.trading.portfolio.infrastructure;

import com.trading.portfolio.domain.model.PortfolioTransaction;
import com.trading.portfolio.domain.model.TransactionType;
import com.trading.portfolio.domain.port.out.PortfolioTransactionRepository;
import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PortfolioTransactionRepositoryAdapter implements PortfolioTransactionRepository {

    private final JpaPortfolioTransactionRepository jpa;

    @Override
    public List<PortfolioTransaction> findByTicker(Ticker ticker) {
        return jpa.findByTicker(ticker.value()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PortfolioTransaction> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public PortfolioTransaction save(PortfolioTransaction transaction) {
        return toDomain(jpa.save(toEntity(transaction)));
    }

    private PortfolioTransaction toDomain(PortfolioTransactionEntity e) {
        return new PortfolioTransaction(
                e.getId(), new Ticker(e.getTicker()),
                TransactionType.valueOf(e.getTransactionType()),
                e.getQuantity(), Money.of(e.getPrice()), Money.of(e.getCommission()),
                e.getExecutedAt());
    }

    private PortfolioTransactionEntity toEntity(PortfolioTransaction t) {
        return PortfolioTransactionEntity.builder()
                .id(t.id()).ticker(t.ticker().value())
                .transactionType(t.transactionType().name())
                .quantity(t.quantity()).price(t.price().amount()).commission(t.commission().amount())
                .executedAt(t.executedAt())
                .build();
    }
}
