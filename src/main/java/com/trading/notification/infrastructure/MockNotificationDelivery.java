package com.trading.notification.infrastructure;

import com.trading.notification.domain.port.out.NotificationDeliveryPort;
import com.trading.shared.kernel.TradingLevels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link NotificationDeliveryPort} stub for the {@code mock} profile — logs the
 * alert instead of sending a real e-mail, so notification flows run offline.
 */
@Slf4j
@Primary
@Profile("mock")
@Component
public class MockNotificationDelivery implements NotificationDeliveryPort {

    @Override
    public void deliver(String ticker, String action, double confidence, String reasoning,
                        String counterThesis, List<String> keyRisks, TradingLevels levels) {
        log.info("[MOCK ALERT] {} {} (confidence={}) levels={} counterThesis={} risks={} — {}",
                action, ticker, confidence, levels, counterThesis, keyRisks, reasoning);
    }

    @Override
    public void deliverMessage(String subject, String body) {
        log.info("[MOCK ALERT] {} — {}", subject, body);
    }
}
