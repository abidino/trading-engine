package com.trading.notification.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "triggered_alerts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_triggered_alert_day",
                columnNames = {"ticker", "alert_type", "triggered_on"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TriggeredAlertEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(name = "alert_type", nullable = false, length = 20)
    private String alertType;

    @Column(name = "triggered_on", nullable = false)
    private LocalDate triggeredOn;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private double level;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private Instant triggeredAt;
}
