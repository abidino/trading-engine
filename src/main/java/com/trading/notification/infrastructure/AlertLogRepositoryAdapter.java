package com.trading.notification.infrastructure;

import com.trading.notification.domain.model.AlertChannel;
import com.trading.notification.domain.model.AlertLog;
import com.trading.notification.domain.model.DeliveryStatus;
import com.trading.notification.domain.port.out.AlertLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface JpaAlertLogRepository extends JpaRepository<AlertLogEntity, UUID> {}

@Component
@RequiredArgsConstructor
public class AlertLogRepositoryAdapter implements AlertLogRepository {

    private final JpaAlertLogRepository jpa;

    @Override
    public AlertLog save(AlertLog log) {
        return toDomain(jpa.save(toEntity(log)));
    }

    @Override
    public List<AlertLog> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    private AlertLog toDomain(AlertLogEntity e) {
        return new AlertLog(e.getId(), e.getTicker(), e.getAction(),
                AlertChannel.valueOf(e.getChannel()), e.getSentAt(),
                DeliveryStatus.valueOf(e.getDeliveryStatus()), e.getErrorMessage());
    }

    private AlertLogEntity toEntity(AlertLog a) {
        return AlertLogEntity.builder()
                .id(a.id()).ticker(a.ticker()).action(a.action())
                .channel(a.channel().name()).sentAt(a.sentAt())
                .deliveryStatus(a.deliveryStatus().name()).errorMessage(a.errorMessage())
                .build();
    }
}
