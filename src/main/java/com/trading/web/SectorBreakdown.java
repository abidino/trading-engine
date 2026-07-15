package com.trading.web;

import java.util.List;

public record SectorBreakdown(String sector, double totalValue, double percentage, List<String> tickers) {}
