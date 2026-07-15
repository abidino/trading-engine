package com.trading.marketdata.domain.model;

/**
 * The directional verdict assigned to a ticker after interpreting its
 * computed technical indicators (moving-average alignment, RSI, MACD).
 *
 * Ordered from most bullish to most bearish.
 */
public enum TrendDirection {
    STRONG_UPTREND,
    UPTREND,
    SIDEWAYS,
    DOWNTREND,
    STRONG_DOWNTREND
}
