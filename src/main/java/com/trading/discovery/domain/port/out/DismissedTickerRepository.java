package com.trading.discovery.domain.port.out;

import java.util.List;

/**
 * Outbound port: the suppression list for discovery.
 *
 * A suppressed ticker is neither re-evaluated (cost saving) nor surfaced in the UI
 * until its deadline passes. Two suppression sources exist:
 *  - {@code DISMISSED}: the user rejected an up-trend candidate.
 *  - {@code AUTO}: the LLM verdict was not an up-trend, so re-analysing it every
 *    daily cycle would be wasteful; it is parked for a cooldown window.
 */
public interface DismissedTickerRepository {

    /** User dismissal — suppressed for the standard cooldown window. */
    void dismiss(String ticker, String reason);

    /** Automatic (non up-trend) suppression — suppressed for the standard cooldown window. */
    void suppress(String ticker, String reason);

    /** True while any suppression (DISMISSED or AUTO) is still within its deadline. */
    boolean isSuppressed(String ticker);

    /** True while an active DISMISSED suppression exists (user-rejected). */
    boolean isDismissed(String ticker);

    /** Tickers with an active DISMISSED suppression. */
    List<String> findAllTickers();

    void remove(String ticker);
}
