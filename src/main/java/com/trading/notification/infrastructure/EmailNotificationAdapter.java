package com.trading.notification.infrastructure;

import com.trading.notification.domain.model.AlertChannel;
import com.trading.notification.domain.port.out.NotificationDeliveryPort;
import com.trading.shared.kernel.TradingLevels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationAdapter implements NotificationDeliveryPort {

    private final JavaMailSender mailSender;

    @Value("${notification.email.to:}")
    private String recipient;

    @Value("${notification.email.from:no-reply@tradingengine.local}")
    private String sender;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.port:0}")
    private int smtpPort;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Override
    public void deliver(String ticker, String action, double confidence, String reasoning,
                        String counterThesis, List<String> keyRisks, TradingLevels levels) {
        if (recipient.isBlank()) {
            log.debug("Email notification skipped — notification.email.to not configured");
            return;
        }

        String subject = "[TradingEngine] %s signal: %s (conf=%.0f%%)".formatted(action, ticker,
                confidence * 100);
        String body = """
                Trading signal generated:
                
                Ticker: %s
                Action: %s
                Confidence: %.1f%%
                %s
                Reasoning:
                %s
                %s
                """.formatted(ticker, action, confidence * 100, formatLevels(levels), reasoning,
                formatCounterView(counterThesis, keyRisks));

        // Let failures propagate so the caller records the alert as FAILED rather than a false SENT.
        sendRawEmail(recipient, subject, body);
        log.info("Email sent to {} for ticker={} action={}", recipient, ticker, action);
    }

    @Override
    public void deliverMessage(String subject, String body) {
        if (recipient.isBlank()) {
            log.debug("Email message skipped — notification.email.to not configured");
            return;
        }
        sendRawEmail(recipient, subject, body);
        log.info("Email message sent to {}: {}", recipient, subject);
    }

    /** Renders the mandatory opposing case + concrete risks so alerts are never one-sided. */
    private String formatCounterView(String counterThesis, List<String> keyRisks) {
        StringBuilder sb = new StringBuilder();
        if (counterThesis != null && !counterThesis.isBlank()) {
            sb.append("\nCounter-thesis (opposing case):\n").append(counterThesis).append("\n");
        }
        if (keyRisks != null && !keyRisks.isEmpty()) {
            sb.append("\nKey risks:\n");
            for (String risk : keyRisks) {
                sb.append("  - ").append(risk).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Sends a raw email using the configured JavaMailSender. Intended for the diagnostic
     * test endpoint and internal reuse. Any SMTP failure propagates as a {@link MailException}
     * so the caller sees the real error instead of a silently-swallowed one.
     *
     * @param to optional recipient override; falls back to the configured alert recipient when blank
     */
    public void sendRawEmail(String to, String subject, String body) {
        String target = (to != null && !to.isBlank()) ? to : recipient;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(sender);
        msg.setTo(target);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }

    /** Effective SMTP configuration for diagnostics (password intentionally excluded). */
    public EmailConfig currentConfig() {
        return new EmailConfig(smtpHost, smtpPort, smtpUsername, sender, recipient);
    }

    public record EmailConfig(String host, int port, String username, String from, String to) {}

    private String formatLevels(TradingLevels l) {
        if (l == null || !l.hasAnyLevel()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\nTrading levels:\n");
        if (l.entryLow() != null || l.entryHigh() != null) {
            sb.append("  Buy zone: ").append(fmt(l.entryLow())).append(" - ").append(fmt(l.entryHigh())).append("\n");
        }
        if (l.aggressiveEntry() != null) sb.append("  Aggressive entry: ").append(fmt(l.aggressiveEntry())).append("\n");
        if (l.idealEntry() != null) sb.append("  Ideal entry: ").append(fmt(l.idealEntry())).append("\n");
        if (l.safeEntry() != null) sb.append("  Safe entry: ").append(fmt(l.safeEntry())).append("\n");
        if (l.stopLoss() != null) sb.append("  Stop-loss: ").append(fmt(l.stopLoss())).append("\n");
        if (l.takeProfit() != null) sb.append("  Take-profit: ").append(fmt(l.takeProfit())).append("\n");
        if (l.nearestSupport() != null) sb.append("  Nearest support: ").append(fmt(l.nearestSupport())).append("\n");
        if (l.nearestResistance() != null) sb.append("  Nearest resistance: ").append(fmt(l.nearestResistance())).append("\n");
        return sb.toString();
    }

    private String fmt(Double v) {
        return v != null ? String.format("%.2f", v) : "n/a";
    }
}
