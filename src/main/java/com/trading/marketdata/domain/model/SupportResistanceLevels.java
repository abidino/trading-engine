package com.trading.marketdata.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Value Object: support &amp; resistance levels computed for a ticker on a given day.
 *
 * <p>Two complementary sets are provided:</p>
 * <ul>
 *   <li><b>Pivot levels</b> — classic floor-trader pivots (P, R1–R3, S1–S3) derived
 *       from the most recent candle's high/low/close.</li>
 *   <li><b>Swing levels</b> — price zones discovered from local swing highs/lows over
 *       the look-back window (clustered so nearby pivots collapse into one level).</li>
 * </ul>
 *
 * {@code nearestSupport} / {@code nearestResistance} are the closest swing (falling back
 * to pivot) levels below / above the current {@code close}, i.e. the actionable ones.
 */
public record SupportResistanceLevels(
        String ticker,
        LocalDate asOfDate,
        double close,
        double pivot,
        double r1,
        double r2,
        double r3,
        double s1,
        double s2,
        double s3,
        List<Double> supports,
        List<Double> resistances,
        Double nearestSupport,
        Double nearestResistance,
        int dataPoints
) {}
