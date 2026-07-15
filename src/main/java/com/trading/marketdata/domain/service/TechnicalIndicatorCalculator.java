package com.trading.marketdata.domain.service;

import com.trading.marketdata.domain.model.PriceCandle;
import com.trading.marketdata.domain.model.TechnicalIndicatorSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure domain service: computes the standard technical indicators from a series
 * of OHLCV candles. No Spring, no IO — fully deterministic and unit-testable.
 *
 * Indicators produced:
 * <ul>
 *   <li>EMA 9 / 20 / 50 / 100 / 200 (exponential moving averages of close)</li>
 *   <li>RSI(14) using Wilder's smoothing</li>
 *   <li>MACD line (EMA12 − EMA26), signal (EMA9 of MACD), histogram (line − signal)</li>
 * </ul>
 *
 * Any indicator whose required look-back exceeds the supplied candle count is
 * returned as {@code null} (see {@link TechnicalIndicatorSnapshot}).
 */
public class TechnicalIndicatorCalculator {

    private static final int MACD_FAST = 12;
    private static final int MACD_SLOW = 26;
    private static final int MACD_SIGNAL = 9;
    private static final int RSI_PERIOD = 14;

    /**
     * @param ticker  the symbol (echoed into the snapshot)
     * @param candles candles in ANY order — they are defensively sorted ascending by date
     * @return the indicator snapshot taken at the most recent candle
     * @throws IllegalArgumentException if {@code candles} is empty
     */
    public TechnicalIndicatorSnapshot calculate(String ticker, List<PriceCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute indicators with no candles for " + ticker);
        }

        List<PriceCandle> sorted = candles.stream()
                .sorted(Comparator.comparing(PriceCandle::candleDate))
                .toList();

        double[] closes = new double[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            closes[i] = sorted.get(i).close().doubleValue();
        }

        PriceCandle last = sorted.get(sorted.size() - 1);

        Double[] macd = macd(closes, MACD_FAST, MACD_SLOW, MACD_SIGNAL);

        return new TechnicalIndicatorSnapshot(
                ticker,
                last.candleDate(),
                last.close(),
                emaLatest(closes, 9),
                emaLatest(closes, 20),
                emaLatest(closes, 50),
                emaLatest(closes, 100),
                emaLatest(closes, 200),
                rsi(closes, RSI_PERIOD),
                macd[0],
                macd[1],
                macd[2],
                sorted.size()
        );
    }

    // -----------------------------------------------------------------------
    // EMA
    // -----------------------------------------------------------------------

    /** Returns the latest EMA value for {@code period}, or null if too few points. */
    private Double emaLatest(double[] values, int period) {
        double[] series = emaSeries(values, period);
        return series == null ? null : round(series[series.length - 1]);
    }

    /**
     * Full EMA series aligned to {@code values} (index 0 is the first point at which
     * the EMA is defined; the returned array has the same length as {@code values}
     * starting at index {@code period-1}). Returns null when {@code values.length < period}.
     */
    private double[] emaSeries(double[] values, int period) {
        if (values.length < period) {
            return null;
        }
        double[] ema = new double[values.length - period + 1];
        double multiplier = 2.0 / (period + 1);

        // Seed with the simple average of the first `period` values.
        double seed = 0;
        for (int i = 0; i < period; i++) {
            seed += values[i];
        }
        seed /= period;
        ema[0] = seed;

        for (int i = period; i < values.length; i++) {
            ema[i - period + 1] = (values[i] - ema[i - period]) * multiplier + ema[i - period];
        }
        return ema;
    }

    // -----------------------------------------------------------------------
    // RSI (Wilder)
    // -----------------------------------------------------------------------

    private Double rsi(double[] closes, int period) {
        if (closes.length < period + 1) {
            return null;
        }

        double gainSum = 0;
        double lossSum = 0;
        for (int i = 1; i <= period; i++) {
            double change = closes[i] - closes[i - 1];
            if (change >= 0) {
                gainSum += change;
            } else {
                lossSum -= change;
            }
        }
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;

        for (int i = period + 1; i < closes.length; i++) {
            double change = closes[i] - closes[i - 1];
            double gain = change > 0 ? change : 0;
            double loss = change < 0 ? -change : 0;
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }

        if (avgLoss == 0) {
            return 100.0;
        }
        double rs = avgGain / avgLoss;
        return round(100.0 - (100.0 / (1.0 + rs)));
    }

    // -----------------------------------------------------------------------
    // MACD
    // -----------------------------------------------------------------------

    /** @return {macdLine, signalLine, histogram} latest values; entries are null when undefined. */
    private Double[] macd(double[] closes, int fast, int slow, int signalPeriod) {
        double[] emaFast = emaSeries(closes, fast);
        double[] emaSlow = emaSeries(closes, slow);
        if (emaFast == null || emaSlow == null) {
            return new Double[]{null, null, null};
        }

        // Align both EMA series to the same (most-recent) tail. emaSlow starts later.
        int offset = (slow - fast); // emaFast is `offset` entries longer than emaSlow
        List<Double> macdList = new ArrayList<>();
        for (int i = 0; i < emaSlow.length; i++) {
            macdList.add(emaFast[i + offset] - emaSlow[i]);
        }

        double macdLatest = macdList.get(macdList.size() - 1);

        double[] macdArr = new double[macdList.size()];
        for (int i = 0; i < macdList.size(); i++) {
            macdArr[i] = macdList.get(i);
        }
        double[] signalSeries = emaSeries(macdArr, signalPeriod);
        if (signalSeries == null) {
            return new Double[]{round(macdLatest), null, null};
        }
        double signalLatest = signalSeries[signalSeries.length - 1];
        double histogram = macdLatest - signalLatest;

        return new Double[]{round(macdLatest), round(signalLatest), round(histogram)};
    }

    // -----------------------------------------------------------------------

    private double round(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }
}
