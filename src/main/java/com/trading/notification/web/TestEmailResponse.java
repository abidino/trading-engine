package com.trading.notification.web;

import java.util.Map;

/**
 * Diagnostic result of a test-email attempt. On success {@code error} is null; on failure the
 * whole exception chain is captured in {@code error} so SMTP problems (auth, TLS, blocked port,
 * Gmail app-password issues) are visible directly in the HTTP response.
 */
public record TestEmailResponse(
        boolean success,
        Map<String, Object> config,
        String error
) {}
