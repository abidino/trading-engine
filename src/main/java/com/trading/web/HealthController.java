package com.trading.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Lightweight, unsecured liveness endpoint for a separately-deployed UI or an
 * external uptime monitor. Always cheap and dependency-free (unlike Actuator's
 * aggregated {@code /actuator/health}, which can report DOWN when an optional
 * subsystem such as mail is unavailable). Permitted without authentication in
 * {@link com.trading.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "trading-engine",
                "timestamp", Instant.now().toString()
        );
    }
}
