package com.trading.orchestration.domain.model;

import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Ticker;

import java.util.Map;
import java.util.Objects;

/**
 * Value Object: input to the AI analysis pipeline.
 * Carries the ticker, the context type, and optional metadata
 * (e.g. entry price and position size for PORTFOLIO_REVIEW).
 */
public record AnalysisRequest(
        Ticker ticker,
        AnalysisRequestType requestType,
        Map<String, String> contextMetadata
) {
    public AnalysisRequest {
        Objects.requireNonNull(ticker, "Ticker must not be null");
        Objects.requireNonNull(requestType, "RequestType must not be null");
        contextMetadata = contextMetadata != null ? Map.copyOf(contextMetadata) : Map.of();
    }

    public static AnalysisRequest of(Ticker ticker, AnalysisRequestType type) {
        return new AnalysisRequest(ticker, type, Map.of());
    }

    /** Convenience accessor for metadata values. */
    public String meta(String key) {
        return contextMetadata.get(key);
    }
}
