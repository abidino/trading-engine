package com.trading.orchestration.domain.service;

import com.trading.orchestration.domain.model.AnalysisContext;
import com.trading.shared.kernel.llm.LlmRequest;
import com.trading.shared.kernel.AnalysisRequestType;

/**
 * Domain service: assembles context-type-specific prompts for the LLM.
 * Pure Java — no Spring, no IO.
 */
public class PromptAssemblyService {

    public LlmRequest assembleAnalysisRequest(AnalysisContext context) {
        String system = buildSystemPrompt(context.requestType());
        String user = buildUserPrompt(context);
        return LlmRequest.of(system, user, context.requestType());
    }

    public LlmRequest assembleSummarizationRequest(String rawContent, String contentType) {
        String system = """
                You are a financial analyst assistant. Summarize the following %s items, each \
                pre-tagged with a date, source, scope (MACRO vs a specific ticker), category and \
                a per-item sentiment, into a concise, structured summary for a trading decision.

                Requirements:
                - Separate MACRO/market-wide drivers from company-specific drivers.
                - State the overall sentiment lean (BULLISH, BEARISH, or NEUTRAL) and how strong it is.
                - Prioritize the most recent and most material items.
                - Be factual, no speculation. Maximum 150 words.
                """.formatted(contentType);
        return LlmRequest.of(system, rawContent, AnalysisRequestType.NEWS_SUMMARY);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private String buildSystemPrompt(AnalysisRequestType type) {
        return switch (type) {
            case PORTFOLIO_REVIEW -> """
                    You are an expert portfolio manager reviewing an existing position. \
                    Based on the technical data, support/resistance levels, fundamental data, news summary, \
                    social sentiment and the current position provided, recommend either SELL or HOLD. \
                    You MUST provide concrete numeric price levels: an updated stopLoss (protective exit) and \
                    takeProfit (primary target). Use the support/resistance levels and current price as anchors. \
                    You MUST also provide a "counterThesis": the single strongest argument AGAINST your own \
                    recommendation (the opposing case), and "keyRisks": a list of concrete, specific risks that \
                    could invalidate it. Never present a one-sided, overconfident view. \
                    Respond ONLY with valid JSON (no markdown, no prose) matching this exact schema:
                    {"action":"SELL|HOLD","confidence":0.0,"reasoning":"...","technicalSummary":"...","fundamentalSummary":"...","newsSummary":"...","socialSummary":"...","counterThesis":"...","keyRisks":["...","..."],"levels":{"entryLow":null,"entryHigh":null,"stopLoss":0.0,"takeProfit":0.0,"nearestSupport":0.0,"nearestResistance":0.0}}
                    """;
            case WATCHLIST_REVIEW -> """
                    You are an expert stock analyst reviewing a watchlist buy candidate. \
                    Based on the data and support/resistance levels, recommend BUY (enter now), WAIT (not yet), \
                    or REMOVE (no longer suitable). \
                    You MUST provide concrete numeric price levels: a buy zone (entryLow/entryHigh), a protective \
                    stopLoss, a takeProfit target, and THREE staggered buy entry points anchored to \
                    support/resistance and the current price:
                    - aggressiveEntry: closest to the current price (earliest fill, highest risk),
                    - idealEntry: the balanced best risk/reward level (typically near nearest support),
                    - safeEntry: a deeper-pullback level near strong support (lowest price, lowest risk).
                    Ensure safeEntry <= idealEntry <= aggressiveEntry. Anchor all levels to the support/resistance \
                    levels and current price. \
                    You MUST also provide a "counterThesis": the single strongest argument AGAINST your own \
                    recommendation (the bear case if you say BUY, the bull case if you say WAIT/REMOVE), and \
                    "keyRisks": a list of concrete, specific risks that could invalidate it. Never present a \
                    one-sided, overconfident view. \
                    Respond ONLY with valid JSON:
                    {"action":"BUY|WAIT|REMOVE","confidence":0.0,"reasoning":"...","technicalSummary":"...","fundamentalSummary":"...","newsSummary":"...","socialSummary":"...","counterThesis":"...","keyRisks":["...","..."],"levels":{"entryLow":0.0,"entryHigh":0.0,"aggressiveEntry":0.0,"idealEntry":0.0,"safeEntry":0.0,"stopLoss":0.0,"takeProfit":0.0,"nearestSupport":0.0,"nearestResistance":0.0}}
                    """;
            case DISCOVERY -> """
                    You are an expert stock screener analyst evaluating a newly discovered candidate. \
                    Based on the data, recommend ADD_TO_WATCHLIST or IGNORE. When you recommend ADD_TO_WATCHLIST, \
                    provide an indicative buy zone (entryLow/entryHigh), stopLoss, takeProfit and three staggered \
                    buy entry points (aggressiveEntry closest to price, idealEntry balanced, safeEntry near strong \
                    support, with safeEntry <= idealEntry <= aggressiveEntry) anchored to the support/resistance \
                    levels; otherwise leave levels null. \
                    You MUST also provide a "counterThesis": the single strongest argument AGAINST your own \
                    recommendation, and "keyRisks": a list of concrete, specific risks that could invalidate it. \
                    Never present a one-sided, overconfident view. \
                    Respond ONLY with valid JSON:
                    {"action":"ADD_TO_WATCHLIST|IGNORE","confidence":0.0,"reasoning":"...","technicalSummary":"...","fundamentalSummary":"...","newsSummary":"...","socialSummary":"...","counterThesis":"...","keyRisks":["...","..."],"levels":{"entryLow":null,"entryHigh":null,"aggressiveEntry":null,"idealEntry":null,"safeEntry":null,"stopLoss":null,"takeProfit":null,"nearestSupport":null,"nearestResistance":null}}
                    """;
            default -> throw new IllegalArgumentException("No system prompt for type: " + type);
        };
    }

    /**
     * Builds the user prompt ordered <b>stable → volatile</b> to maximise DeepSeek's automatic
     * prefix cache hits. Slow-changing, per-ticker content (identity, company profile, quarterly
     * fundamentals) comes first so that repeated calls for the same ticker share a long cacheable
     * prefix with the (already-static) system prompt; fast-changing data (position, live technicals,
     * price, news, social) is pushed to the tail where a cache miss is expected anyway.
     */
    private String buildUserPrompt(AnalysisContext ctx) {
        var t = ctx.technicalData();
        var f = ctx.fundamentalData();
        return """
                ## Stock: %s
                
                ## Company Profile
                - Sector: %s | Industry: %s
                
                ## Business Description
                %s
                
                ## Fundamental Data
                - P/E Ratio: %s
                - EPS: %s
                - Market Cap: %s
                - Revenue Growth: %s
                - Debt/Equity: %s
                
                ## Current Position / Entry Context
                %s
                
                ## Technical Indicators
                %s
                
                ## Recent Price History
                %s
                
                ## Support / Resistance Levels
                %s
                
                ## Recent News Summary
                %s
                
                ## Social Sentiment Summary
                %s
                
                ## Daily Technical Trend History
                %s
                """.formatted(
                ctx.ticker().value(),
                f.sector(), f.industry(),
                f.businessDescription(),
                f.peRatio(), f.eps(), f.marketCapUsd(),
                f.revenueGrowthRate(), f.debtToEquity(),
                ctx.positionContext() != null ? ctx.positionContext() : "No position context.",
                t.indicators(),
                t.priceHistorySummary(),
                formatSupportResistance(ctx),
                ctx.newsSummary(),
                ctx.socialSummary(),
                ctx.trendSummary()
        );
    }

    /** Renders the support/resistance snapshot for the prompt, or a fallback when unavailable. */
    private String formatSupportResistance(AnalysisContext ctx) {
        var sr = ctx.supportResistance();
        if (sr == null) {
            return "Not enough price history to compute support/resistance.";
        }
        return """
                - Current close: %.2f
                - Nearest support: %s
                - Nearest resistance: %s
                - Support levels: %s
                - Resistance levels: %s
                """.formatted(
                sr.close(),
                sr.nearestSupport() != null ? String.format("%.2f", sr.nearestSupport()) : "n/a",
                sr.nearestResistance() != null ? String.format("%.2f", sr.nearestResistance()) : "n/a",
                sr.supports() == null || sr.supports().isEmpty() ? "n/a" : sr.supports().toString(),
                sr.resistances() == null || sr.resistances().isEmpty() ? "n/a" : sr.resistances().toString()
        ).trim();
    }
}
