package com.trading.notification.domain.model;

/**
 * Category of a proactive threshold alert derived from a ticker's analysis levels
 * or portfolio state. Used together with the ticker + date to de-duplicate so the
 * same condition fires at most once per day.
 */
public enum AlertType {
    /** Live price entered the suggested buy zone / ideal or safe entry. */
    ENTRY_ZONE,
    /** Live price broke below the stop-loss level. */
    STOP_LOSS,
    /** Live price reached the take-profit target. */
    TAKE_PROFIT,
    /** Whole portfolio dropped by more than the configured daily threshold. */
    PORTFOLIO_DROP
}
