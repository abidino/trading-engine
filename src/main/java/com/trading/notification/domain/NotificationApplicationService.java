package com.trading.notification.domain;

import com.trading.notification.domain.model.AlertChannel;
import com.trading.notification.domain.model.AlertLog;
import com.trading.notification.domain.port.out.AlertLogRepository;
import com.trading.notification.domain.port.out.NotificationDeliveryPort;
import com.trading.shared.kernel.event.DecisionProduced;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Listens to DecisionProduced events and sends alerts based on configurable rules.
 * Only alerts when action is high-urgency AND confidence exceeds threshold.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationApplicationService {

    private final NotificationDeliveryPort emailDelivery;
    private final AlertLogRepository alertLogRepository;

    @Value("${notification.confidence-threshold:0.70}")
    private double confidenceThreshold;

    @Value("${notification.alert-actions:BUY,SELL}")
    private List<String> alertActions;

    @EventListener
    public void onDecisionProduced(DecisionProduced event) {
        boolean shouldAlert = alertActions.contains(event.action())
                && event.confidence() >= confidenceThreshold;

        if (!shouldAlert) {
            alertLogRepository.save(
                    AlertLog.skipped(event.ticker().value(), event.action(), AlertChannel.EMAIL));
            return;
        }

        try {
            emailDelivery.deliver(
                    event.ticker().value(), event.action(),
                    event.confidence(), event.reasoning(),
                    event.counterThesis(), event.keyRisks(), event.levels());
            alertLogRepository.save(
                    AlertLog.sent(event.ticker().value(), event.action(), AlertChannel.EMAIL));
            log.info("Alert sent: ticker={} action={}", event.ticker(), event.action());
        } catch (Exception e) {
            alertLogRepository.save(
                    AlertLog.failed(event.ticker().value(), event.action(),
                            AlertChannel.EMAIL, e.getMessage()));
            log.error("Alert delivery failed for {}", event.ticker(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<AlertLog> listAll() {
        return alertLogRepository.findAll();
    }
}
