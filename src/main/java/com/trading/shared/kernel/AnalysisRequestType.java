package com.trading.shared.kernel;

/**
 * The type of analysis being requested.
 * Shared across all domains — drives prompt selection and LLM routing.
 */
public enum AnalysisRequestType {
    /** Review an existing portfolio position (SELL / HOLD). */
    PORTFOLIO_REVIEW,
    /** Review a watchlist candidate (BUY / WAIT / REMOVE). */
    WATCHLIST_REVIEW,
    /** Evaluate a newly discovered stock (ADD_TO_WATCHLIST / IGNORE). */
    DISCOVERY,
    /** Internal: summarise raw news headlines via LLM. */
    NEWS_SUMMARY,
    /** Internal: summarise raw social signals via LLM. */
    SOCIAL_SUMMARY,
    /** Interpret computed technical indicators into a daily trend verdict. */
    TECHNICAL_TREND
}
