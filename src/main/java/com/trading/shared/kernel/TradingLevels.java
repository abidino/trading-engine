package com.trading.shared.kernel;

/**
 * Value Object: actionable price levels attached to an analysis decision.
 *
 * <p>All fields are nullable — a given analysis may not produce every level (e.g. a HOLD/IGNORE
 * decision may omit entry/stop/target). Levels are expressed in the instrument's quote currency.</p>
 *
 * <ul>
 *   <li>{@code entryLow}/{@code entryHigh} — suggested buy zone ("alınabilecek seviye").</li>
 *   <li>{@code aggressiveEntry} — closest-to-current buy point (highest price, earliest fill, most risk).</li>
 *   <li>{@code idealEntry} — balanced buy point (best risk/reward).</li>
 *   <li>{@code safeEntry} — deep-pullback buy point near strong support (lowest price, lowest risk).</li>
 *   <li>{@code stopLoss} — protective exit if the thesis fails.</li>
 *   <li>{@code takeProfit} — primary profit target.</li>
 *   <li>{@code nearestSupport}/{@code nearestResistance} — closest structural levels used as context.</li>
 * </ul>
 */
public record TradingLevels(
        Double entryLow,
        Double entryHigh,
        Double aggressiveEntry,
        Double idealEntry,
        Double safeEntry,
        Double stopLoss,
        Double takeProfit,
        Double nearestSupport,
        Double nearestResistance
) {
    public static TradingLevels empty() {
        return new TradingLevels(null, null, null, null, null, null, null, null, null);
    }

    public boolean hasAnyLevel() {
        return entryLow != null || entryHigh != null
                || aggressiveEntry != null || idealEntry != null || safeEntry != null
                || stopLoss != null || takeProfit != null
                || nearestSupport != null || nearestResistance != null;
    }
}
