package com.trading.notification.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertLogEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false, length = 30)
    private String channel;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private Instant sentAt;

    @Column(nullable = false, length = 20)
    private String deliveryStatus;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
