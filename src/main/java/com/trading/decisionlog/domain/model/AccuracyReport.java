package com.trading.decisionlog.domain.model;

import java.util.List;

/**
 * Aggregated accuracy of past AI decisions once their outcomes have been evaluated.
 * Hit-rate = validated / (validated + invalidated); pending decisions are excluded.
 */
public record AccuracyReport(
        long total,
        long validated,
        long invalidated,
        long pending,
        double hitRate,
        List<Bucket> byAction,
        List<Bucket> byType
) {
    /** Per-dimension breakdown (e.g. per action, per request type). */
    public record Bucket(String key, long validated, long invalidated, double hitRate) {
        public static Bucket of(String key, long validated, long invalidated) {
            long evaluated = validated + invalidated;
            double rate = evaluated == 0 ? 0.0 : (double) validated / evaluated;
            return new Bucket(key, validated, invalidated, rate);
        }
    }

    public static AccuracyReport of(long validated, long invalidated, long pending,
                                    List<Bucket> byAction, List<Bucket> byType) {
        long evaluated = validated + invalidated;
        double rate = evaluated == 0 ? 0.0 : (double) validated / evaluated;
        return new AccuracyReport(validated + invalidated + pending, validated, invalidated,
                pending, rate, byAction, byType);
    }
}
