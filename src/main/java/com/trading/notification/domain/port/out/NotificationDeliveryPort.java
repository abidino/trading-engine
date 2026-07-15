package com.trading.notification.domain.port.out;

import com.trading.shared.kernel.TradingLevels;

import java.util.List;

/**
 * Outbound port: delivers a notification message via a specific channel.
 * Each channel (email, push, Slack) has its own implementation in infrastructure.
 */
public interface NotificationDeliveryPort {
    void deliver(String ticker, String action, double confidence, String reasoning,
                 String counterThesis, List<String> keyRisks, TradingLevels levels);

    /**
     * Delivers a plain subject/body message (used by threshold alerts, reports, etc.).
     * Implementations must let delivery failures propagate so callers can record FAILED.
     */
    void deliverMessage(String subject, String body);
}
