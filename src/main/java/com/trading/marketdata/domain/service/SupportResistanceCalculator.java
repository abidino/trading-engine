package com.trading.marketdata.domain.service;

import com.trading.marketdata.domain.model.PriceCandle;
import com.trading.marketdata.domain.model.SupportResistanceLevels;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure domain service: derives support &amp; resistance levels from OHLCV candles.
 * No Spring, no IO — fully deterministic and unit-testable.
 *
 * <p>Method:</p>
 * <ol>
 *   <li><b>Classic pivots</b> from the most recent candle (H/L/C): P, R1–R3, S1–S3.</li>
 *   <li><b>Swing points</b>: a candle high is a swing high if it is the max within a
 *       {@code ±FRACTAL} window; symmetrically for swing lows. Nearby swing prices are
 *       clustered (within {@code CLUSTER_PCT}) into a single representative level so the
 *       output is a short list of meaningful zones rather than dozens of noisy pivots.</li>
 *   <li>Levels below the latest close become supports; levels above become resistances.
 *       {@code nearestSupport}/{@code nearestResistance} are the closest of each.</li>
 * </ol>
 */
public class SupportResistanceCalculator {

    /** Half-window size for fractal swing detection (candles on each side). */
    private static final int FRACTAL = 3;
    /** Two levels within this fraction of each other are merged into one. */
    private static final double CLUSTER_PCT = 0.015; // 1.5%

    public SupportResistanceLevels calculate(String ticker, List<PriceCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute support/resistance with no candles for " + ticker);
        }

        List<PriceCandle> sorted = candles.stream()
                .sorted(Comparator.comparing(PriceCandle::candleDate))
                .toList();

        PriceCandle last = sorted.get(sorted.size() - 1);
        double high = last.high().doubleValue();
        double low = last.low().doubleValue();
        double close = last.close().doubleValue();

        // --- classic floor-trader pivots ---
        double pivot = (high + low + close) / 3.0;
        double range = high - low;
        double r1 = 2 * pivot - low;
        double s1 = 2 * pivot - high;
        double r2 = pivot + range;
        double s2 = pivot - range;
        double r3 = high + 2 * (pivot - low);
        double s3 = low - 2 * (high - pivot);

        // --- swing highs / lows ---
        List<Double> swingHighs = new ArrayList<>();
        List<Double> swingLows = new ArrayList<>();
        for (int i = FRACTAL; i < sorted.size() - FRACTAL; i++) {
            double h = sorted.get(i).high().doubleValue();
            double l = sorted.get(i).low().doubleValue();
            boolean isHigh = true;
            boolean isLow = true;
            for (int j = i - FRACTAL; j <= i + FRACTAL; j++) {
                if (j == i) continue;
                if (sorted.get(j).high().doubleValue() > h) isHigh = false;
                if (sorted.get(j).low().doubleValue() < l) isLow = false;
            }
            if (isHigh) swingHighs.add(h);
            if (isLow) swingLows.add(l);
        }

        List<Double> levels = new ArrayList<>();
        levels.addAll(swingHighs);
        levels.addAll(swingLows);
        List<Double> clustered = cluster(levels);

        List<Double> supports = new ArrayList<>();
        List<Double> resistances = new ArrayList<>();
        for (double level : clustered) {
            if (level < close) {
                supports.add(round(level));
            } else if (level > close) {
                resistances.add(round(level));
            }
        }
        // Supports: highest (closest below) first. Resistances: lowest (closest above) first.
        supports.sort(Comparator.reverseOrder());
        resistances.sort(Comparator.naturalOrder());

        Double nearestSupport = supports.isEmpty() ? round(s1) : supports.get(0);
        Double nearestResistance = resistances.isEmpty() ? round(r1) : resistances.get(0);

        return new SupportResistanceLevels(
                ticker,
                last.candleDate(),
                round(close),
                round(pivot),
                round(r1), round(r2), round(r3),
                round(s1), round(s2), round(s3),
                supports, resistances,
                nearestSupport, nearestResistance,
                sorted.size());
    }

    /** Collapses levels that are within {@link #CLUSTER_PCT} of each other into their average. */
    private List<Double> cluster(List<Double> levels) {
        List<Double> sorted = new ArrayList<>(levels);
        sorted.sort(Comparator.naturalOrder());

        List<Double> result = new ArrayList<>();
        int i = 0;
        while (i < sorted.size()) {
            double sum = sorted.get(i);
            int count = 1;
            int j = i + 1;
            while (j < sorted.size()
                    && Math.abs(sorted.get(j) - sorted.get(i)) / sorted.get(i) <= CLUSTER_PCT) {
                sum += sorted.get(j);
                count++;
                j++;
            }
            result.add(sum / count);
            i = j;
        }
        return result;
    }

    private double round(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }
}
