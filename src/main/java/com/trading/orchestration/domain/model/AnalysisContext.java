package com.trading.orchestration.domain.model;

import com.trading.orchestration.domain.port.out.LivePricePort;
import com.trading.orchestration.domain.port.out.SupportResistancePort;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Ticker;

/**
 * Aggregate: assembled snapshot of all data signals for a single analysis run.
 * Passed to the PromptAssemblyService to build the final LLM prompt.
 */
public record AnalysisContext(
        Ticker ticker,
        AnalysisRequestType requestType,
        TechnicalData technicalData,
        FundamentalData fundamentalData,
        String newsSummary,
        String socialSummary,
        String trendSummary,
        SupportResistancePort.Snapshot supportResistance,
        LivePricePort.Snapshot liveQuote,
        String positionContext
) {}
