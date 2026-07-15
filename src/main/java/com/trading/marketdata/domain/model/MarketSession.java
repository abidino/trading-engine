package com.trading.marketdata.domain.model;

/**
 * US equity trading session for an intraday quote.
 */
public enum MarketSession {
    /** Pre-market (typically 04:00–09:30 US/Eastern). */
    PRE_MARKET,
    /** Regular session (09:30–16:00 US/Eastern). */
    REGULAR,
    /** After-hours / post-market (16:00–20:00 US/Eastern). */
    POST_MARKET,
    /** Outside any trading session (overnight). */
    CLOSED
}
