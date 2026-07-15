package com.trading.marketdata.web;

import com.trading.marketdata.domain.model.SupportResistanceLevels;

import java.util.List;

/** API view of computed support &amp; resistance levels. */
public record SupportResistanceResponse(
        String ticker,
        String asOfDate,
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
) {
    public static SupportResistanceResponse from(SupportResistanceLevels l) {
        return new SupportResistanceResponse(
                l.ticker(),
                l.asOfDate().toString(),
                l.close(),
                l.pivot(),
                l.r1(), l.r2(), l.r3(),
                l.s1(), l.s2(), l.s3(),
                l.supports(), l.resistances(),
                l.nearestSupport(), l.nearestResistance(),
                l.dataPoints());
    }
}
