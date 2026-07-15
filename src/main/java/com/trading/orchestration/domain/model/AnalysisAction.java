package com.trading.orchestration.domain.model;

/** All possible actions the AI engine can recommend. */
public enum AnalysisAction {
    /** Portfolio review: sell the position now. */
    SELL,
    /** Portfolio review: keep holding the position. */
    HOLD,
    /** Watchlist review: enter the position now. */
    BUY,
    /** Watchlist review: conditions not right yet, continue monitoring. */
    WAIT,
    /** Watchlist review: the stock is no longer a good candidate, remove. */
    REMOVE,
    /** Discovery: add to watchlist for further monitoring. */
    ADD_TO_WATCHLIST,
    /** Discovery: not worth tracking, ignore. */
    IGNORE
}
