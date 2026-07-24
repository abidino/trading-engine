package com.trading.notification.infrastructure;

import com.trading.notification.domain.port.out.NotificationDeliveryPort;
import com.trading.shared.kernel.TradingLevels;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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
        String html = buildSignalHtml(ticker, action, confidence, reasoning, counterThesis, keyRisks, levels);
        String text = buildSignalText(ticker, action, confidence, reasoning, counterThesis, keyRisks, levels);

        // Let failures propagate so the caller records the alert as FAILED rather than a false SENT.
        sendHtmlEmail(recipient, subject, html, text);
        log.info("Email sent to {} for ticker={} action={}", recipient, ticker, action);
    }

    @Override
    public void deliverMessage(String subject, String body) {
        if (recipient.isBlank()) {
            log.debug("Email message skipped — notification.email.to not configured");
            return;
        }
        sendHtmlEmail(recipient, subject, buildSimpleHtml(subject, body), body);
        log.info("Email message sent to {}: {}", recipient, subject);
    }

    // -----------------------------------------------------------------------
    // Sending
    // -----------------------------------------------------------------------

    /**
     * Sends a multipart (HTML + plain-text fallback) email. Any SMTP failure propagates as a
     * {@link MailException} so the caller sees the real error instead of a silently-swallowed one.
     *
     * @param to optional recipient override; falls back to the configured alert recipient when blank
     */
    public void sendHtmlEmail(String to, String subject, String html, String text) {
        String target = (to != null && !to.isBlank()) ? to : recipient;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(sender);
            helper.setTo(target);
            helper.setSubject(subject);
            helper.setText(text, html); // (plain, html) → multipart/alternative
            mailSender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Failed to build email message: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a raw plain-text email. Kept for the diagnostic test endpoint and internal reuse.
     * Any SMTP failure propagates as a {@link MailException}.
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

    // -----------------------------------------------------------------------
    // HTML rendering
    // -----------------------------------------------------------------------

    private String buildSignalHtml(String ticker, String action, double confidence, String reasoning,
                                   String counterThesis, List<String> keyRisks, TradingLevels levels) {
        String badgeColor = actionColor(action);
        StringBuilder body = new StringBuilder();

        body.append("""
                <tr><td style="padding:0 28px 8px;">
                  <span style="display:inline-block;padding:6px 14px;border-radius:999px;background:%s;color:#ffffff;font-size:13px;font-weight:700;letter-spacing:.5px;">%s</span>
                  <span style="margin-left:10px;font-size:22px;font-weight:700;color:#0f172a;vertical-align:middle;">%s</span>
                </td></tr>
                <tr><td style="padding:0 28px 18px;color:#475569;font-size:13px;">Confidence: <strong style="color:#0f172a;">%.1f%%</strong></td></tr>
                """.formatted(badgeColor, esc(action), esc(ticker), confidence * 100));

        body.append(levelsHtml(levels));

        if (reasoning != null && !reasoning.isBlank()) {
            body.append(sectionHtml("Reasoning", "#0f172a", paragraph(reasoning)));
        }
        if (counterThesis != null && !counterThesis.isBlank()) {
            body.append(sectionHtml("Counter-thesis (opposing case)", "#b45309", paragraph(counterThesis)));
        }
        if (keyRisks != null && !keyRisks.isEmpty()) {
            StringBuilder risks = new StringBuilder("<ul style=\"margin:0;padding-left:18px;color:#475569;font-size:14px;line-height:1.6;\">");
            for (String r : keyRisks) {
                risks.append("<li>").append(esc(r)).append("</li>");
            }
            risks.append("</ul>");
            body.append(sectionHtml("Key risks", "#dc2626", risks.toString()));
        }

        return htmlShell(body.toString());
    }

    /** Renders the actionable price levels as a compact two-column table. */
    private String levelsHtml(TradingLevels l) {
        if (l == null || !l.hasAnyLevel()) {
            return "";
        }
        StringBuilder rows = new StringBuilder();
        if (l.entryLow() != null || l.entryHigh() != null) {
            rows.append(levelRow("Buy zone", fmt(l.entryLow()) + " – " + fmt(l.entryHigh()), "#0f172a"));
        }
        if (l.aggressiveEntry() != null) rows.append(levelRow("Aggressive entry", fmt(l.aggressiveEntry()), "#d97706"));
        if (l.idealEntry() != null) rows.append(levelRow("Ideal entry", fmt(l.idealEntry()), "#059669"));
        if (l.safeEntry() != null) rows.append(levelRow("Safe entry", fmt(l.safeEntry()), "#2563eb"));
        if (l.stopLoss() != null) rows.append(levelRow("Stop-loss", fmt(l.stopLoss()), "#dc2626"));
        if (l.takeProfit() != null) rows.append(levelRow("Take-profit", fmt(l.takeProfit()), "#0284c7"));
        if (l.nearestSupport() != null) rows.append(levelRow("Nearest support", fmt(l.nearestSupport()), "#64748b"));
        if (l.nearestResistance() != null) rows.append(levelRow("Nearest resistance", fmt(l.nearestResistance()), "#64748b"));

        String table = """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;overflow:hidden;">
                %s
                </table>
                """.formatted(rows.toString());
        return sectionHtml("Trading levels", "#0f172a", table);
    }

    private String levelRow(String label, String value, String valueColor) {
        return """
                <tr>
                  <td style="padding:10px 14px;font-size:13px;color:#64748b;border-bottom:1px solid #e2e8f0;">%s</td>
                  <td style="padding:10px 14px;font-size:14px;font-weight:700;color:%s;text-align:right;border-bottom:1px solid #e2e8f0;">%s</td>
                </tr>
                """.formatted(esc(label), valueColor, esc(value));
    }

    private String sectionHtml(String title, String titleColor, String innerHtml) {
        return """
                <tr><td style="padding:8px 28px 4px;">
                  <div style="font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.6px;color:%s;margin-bottom:8px;">%s</div>
                  %s
                </td></tr>
                """.formatted(titleColor, esc(title), innerHtml);
    }

    private String paragraph(String text) {
        return "<p style=\"margin:0 0 6px;color:#475569;font-size:14px;line-height:1.6;\">"
                + esc(text) + "</p>";
    }

    private String buildSimpleHtml(String subject, String body) {
        String inner = """
                <tr><td style="padding:4px 28px 20px;">
                  <p style="margin:0;color:#475569;font-size:15px;line-height:1.6;white-space:pre-wrap;">%s</p>
                </td></tr>
                """.formatted(esc(body));
        String header = """
                <tr><td style="padding:0 28px 16px;">
                  <span style="font-size:20px;font-weight:700;color:#0f172a;">%s</span>
                </td></tr>
                """.formatted(esc(subject));
        return htmlShell(header + inner);
    }

    /** Wraps section rows in the shared responsive email chrome (header + footer). */
    private String htmlShell(String innerRows) {
        return """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#eef2f7;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#eef2f7;padding:24px 0;">
                  <tr><td align="center">
                    <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="width:600px;max-width:92%%;background:#ffffff;border-radius:14px;overflow:hidden;box-shadow:0 1px 3px rgba(15,23,42,.08);">
                      <tr><td style="background:#0f172a;padding:20px 28px;">
                        <span style="color:#ffffff;font-size:16px;font-weight:700;letter-spacing:.5px;">📈 TradingEngine</span>
                      </td></tr>
                      <tr><td style="height:20px;"></td></tr>
                      %s
                      <tr><td style="height:8px;"></td></tr>
                      <tr><td style="padding:16px 28px 24px;border-top:1px solid #eef2f7;color:#94a3b8;font-size:11px;line-height:1.5;">
                        Automated signal from TradingEngine. Not financial advice — do your own research.
                      </td></tr>
                    </table>
                  </td></tr>
                </table>
                </body></html>
                """.formatted(innerRows);
    }

    private String actionColor(String action) {
        if (action == null) return "#475569";
        return switch (action.trim().toUpperCase()) {
            case "BUY", "ADD_TO_WATCHLIST" -> "#059669";
            case "SELL", "REMOVE" -> "#dc2626";
            case "HOLD" -> "#2563eb";
            case "WAIT" -> "#d97706";
            default -> "#475569";
        };
    }

    // -----------------------------------------------------------------------
    // Plain-text fallback rendering
    // -----------------------------------------------------------------------

    private String buildSignalText(String ticker, String action, double confidence, String reasoning,
                                   String counterThesis, List<String> keyRisks, TradingLevels levels) {
        return """
                Trading signal generated:
                
                Ticker: %s
                Action: %s
                Confidence: %.1f%%
                %s
                Reasoning:
                %s
                %s
                """.formatted(ticker, action, confidence * 100, formatLevelsText(levels), reasoning,
                formatCounterViewText(counterThesis, keyRisks));
    }

    /** Renders the mandatory opposing case + concrete risks so alerts are never one-sided. */
    private String formatCounterViewText(String counterThesis, List<String> keyRisks) {
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

    private String formatLevelsText(TradingLevels l) {
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

    /** Minimal HTML escaping for user/LLM-provided text embedded in the template. */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
