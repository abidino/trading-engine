package com.trading.notification.web;

import com.trading.notification.domain.NotificationApplicationService;
import com.trading.notification.infrastructure.EmailNotificationAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationApplicationService service;
    private final EmailNotificationAdapter emailAdapter;
    private final com.trading.notification.domain.AlertEvaluationService alertEvaluationService;

    @GetMapping("/alerts")
    public ResponseEntity<List<AlertResponse>> listAlerts() {
        return ResponseEntity.ok(service.listAll().stream().map(AlertResponse::from).toList());
    }

    /** Recent proactive threshold alerts (stop-loss / take-profit / entry-zone / portfolio-drop). */
    @GetMapping("/alerts/triggered")
    public ResponseEntity<List<TriggeredAlertResponse>> listTriggered(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(alertEvaluationService.listRecent(limit).stream()
                .map(TriggeredAlertResponse::from).toList());
    }

    /**
     * Diagnostic: sends a real email using the configured SMTP settings and reports the outcome.
     * Unlike the alert flow, SMTP failures are surfaced verbatim (HTTP 500 + full error chain)
     * so a "sent but never arrived" situation can be debugged directly.
     *
     * <pre>
     * curl -X POST localhost:4650/api/v1/notifications/test-email \
     *   -H 'Content-Type: application/json' -d '{"to":"you@example.com"}'
     * </pre>
     */
    @PostMapping("/test-email")
    public ResponseEntity<TestEmailResponse> testEmail(@RequestBody(required = false) TestEmailRequest req) {
        var cfg = emailAdapter.currentConfig();
        Map<String, Object> configView = new LinkedHashMap<>();
        configView.put("host", cfg.host());
        configView.put("port", cfg.port());
        configView.put("username", cfg.username());
        configView.put("from", cfg.from());
        configView.put("to", (req != null && req.to() != null && !req.to().isBlank()) ? req.to() : cfg.to());

        String subject = (req != null && req.subject() != null && !req.subject().isBlank())
                ? req.subject()
                : "[TradingEngine] Test email " + Instant.now();
        String body = (req != null && req.body() != null && !req.body().isBlank())
                ? req.body()
                : "This is a test email from TradingEngine to verify SMTP configuration.\nSent at " + Instant.now();

        try {
            emailAdapter.sendRawEmail(req != null ? req.to() : null, subject, body);
            log.info("Test email dispatched to {}", configView.get("to"));
            return ResponseEntity.ok(new TestEmailResponse(true, configView, null));
        } catch (Exception e) {
            log.error("Test email failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TestEmailResponse(false, configView, describe(e)));
        }
    }

    /** Flattens the exception cause chain into a single readable string. */
    private String describe(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (!sb.isEmpty()) {
                sb.append(" -> caused by: ");
            }
            sb.append(cur.getClass().getSimpleName()).append(": ").append(cur.getMessage());
            if (cur == cur.getCause()) {
                break;
            }
        }
        return sb.toString();
    }
}
