package com.trading.orchestration.web;

import com.trading.shared.kernel.AnalysisRequestType;
import jakarta.validation.constraints.NotBlank;

/**
 * Manual analysis trigger. {@code requestType} is optional: when omitted the controller
 * auto-detects it from where the ticker lives (portfolio → PORTFOLIO_REVIEW,
 * watchlist → WATCHLIST_REVIEW, otherwise DISCOVERY).
 */
public record RunRequest(@NotBlank String ticker, AnalysisRequestType requestType) {}
