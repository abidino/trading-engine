package com.trading.discovery.domain.port.out;

import com.trading.discovery.domain.model.DiscoveryFilter;
import com.trading.discovery.domain.model.PotentialStock;

import java.util.List;

/**
 * Outbound port: external stock screener (Finviz or similar).
 * Implemented in infrastructure by FinvizScreenerAdapter.
 */
public interface StockScreenerPort {
    List<PotentialStock> screen(DiscoveryFilter filter);
}
