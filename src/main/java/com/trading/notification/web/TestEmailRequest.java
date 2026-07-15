package com.trading.notification.web;

/**
 * Request body for the diagnostic test-email endpoint. All fields optional:
 * {@code to} falls back to the configured alert recipient, and subject/body
 * default to a canned test message.
 */
public record TestEmailRequest(String to, String subject, String body) {}
