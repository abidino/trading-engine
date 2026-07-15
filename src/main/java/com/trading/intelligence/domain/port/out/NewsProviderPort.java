package com.trading.intelligence.domain.port.out;

import com.trading.intelligence.domain.model.RawNewsArticle;

import java.util.List;

/** Outbound port: external news provider (NewsAPI, etc.). */
public interface NewsProviderPort {

    /** Recent articles mentioning a specific ticker / company. */
    List<RawNewsArticle> fetchForTicker(String ticker, int limit);

    /** Recent macro market-moving headlines (Fed, economy, politics). */
    List<RawNewsArticle> fetchMacroNews(int limit);
}
