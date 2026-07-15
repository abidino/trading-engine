package com.trading.marketdata.domain.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MarketDataApplicationService {
    Map<String, BigDecimal> getLatestPriceForTickers(List<String> tickers);
}
