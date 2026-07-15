package com.trading.marketdata.web;

import com.trading.marketdata.domain.model.PriceCandle;

public record PriceCandleResponse(String ticker, String date, double open,
                                   double high, double low, double close, long volume) {
    public static PriceCandleResponse from(PriceCandle c) {
        return new PriceCandleResponse(c.ticker(), c.candleDate().toString(),
                c.open().doubleValue(), c.high().doubleValue(),
                c.low().doubleValue(), c.close().doubleValue(), c.volume());
    }
}
