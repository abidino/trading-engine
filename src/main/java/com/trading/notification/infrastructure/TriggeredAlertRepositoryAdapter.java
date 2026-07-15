package com.trading.notification.infrastructure;

import com.trading.notification.domain.model.AlertType;
import com.trading.notification.domain.model.TriggeredAlert;
import com.trading.notification.domain.port.out.TriggeredAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
interface JpaTriggeredAlertRepository extends JpaRepository<TriggeredAlertEntity, UUID> {
    boolean existsByTickerAndAlertTypeAndTriggeredOn(String ticker, String alertType, LocalDate triggeredOn);
    List<TriggeredAlertEntity> findAllByOrderByTriggeredAtDesc(PageRequest page);
}

@Component
@RequiredArgsConstructor
public class TriggeredAlertRepositoryAdapter implements TriggeredAlertRepository {

    private final JpaTriggeredAlertRepository jpa;

    @Override
    public TriggeredAlert save(TriggeredAlert alert) {
        return toDomain(jpa.save(toEntity(alert)));
    }

    @Override
    public boolean existsForDay(String ticker, AlertType alertType, LocalDate day) {
        return jpa.existsByTickerAndAlertTypeAndTriggeredOn(ticker, alertType.name(), day);
    }

    @Override
    public List<TriggeredAlert> findRecent(int limit) {
        return jpa.findAllByOrderByTriggeredAtDesc(PageRequest.of(0, Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    private TriggeredAlert toDomain(TriggeredAlertEntity e) {
        return new TriggeredAlert(e.getId(), e.getTicker(), AlertType.valueOf(e.getAlertType()),
                e.getTriggeredOn(), e.getPrice(), e.getLevel(), e.getMessage(), e.getTriggeredAt());
    }

    private TriggeredAlertEntity toEntity(TriggeredAlert a) {
        return TriggeredAlertEntity.builder()
                .id(a.id()).ticker(a.ticker()).alertType(a.alertType().name())
                .triggeredOn(a.triggeredOn()).price(a.price()).level(a.level())
                .message(a.message()).triggeredAt(a.triggeredAt())
                .build();
    }
}
