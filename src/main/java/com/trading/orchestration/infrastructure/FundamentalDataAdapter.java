package com.trading.orchestration.infrastructure;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.trading.orchestration.domain.model.FundamentalData;
import com.trading.orchestration.domain.port.out.FundamentalDataPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fetches fundamental data from Yahoo Finance's quoteSummary endpoint.
 */
@Slf4j
@Component
public class FundamentalDataAdapter implements FundamentalDataPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FundamentalDataAdapter(
            @Value("${yahoo.base-url:https://query1.finance.yahoo.com}") String baseUrl,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "TradingEngine/1.0")
                .build();
    }

    @Override
    public FundamentalData fetchForTicker(String ticker) {
        try {
            String modules = "financialData,summaryProfile,defaultKeyStatistics";
            String raw = restClient.get()
                    .uri("/v10/finance/quoteSummary/{ticker}?modules={modules}", ticker, modules)
                    .retrieve()
                    .body(String.class);
            return parse(ticker, raw);
        } catch (Exception e) {
            log.warn("FundamentalData fetch failed for {}: {}", ticker, e.getMessage());
            return FundamentalData.empty(ticker);
        }
    }

    private FundamentalData parse(String ticker, String json) throws Exception {
        JsonNode root = objectMapper.readTree(json).path("quoteSummary").path("result").path(0);
        JsonNode fin = root.path("financialData");
        JsonNode stats = root.path("defaultKeyStatistics");
        JsonNode profile = root.path("summaryProfile");

        return new FundamentalData(
                ticker,
                nullableDbl(stats, "forwardPE"),
                nullableDbl(stats, "trailingEps"),
                nullableLong(stats, "marketCap"),
                nullableDbl(stats, "revenueGrowth"),
                nullableDbl(fin, "debtToEquity"),
                profile.path("sector").asString(null),
                profile.path("industry").asString(null),
                profile.path("longBusinessSummary").asString(null)
        );
    }

    private Double nullableDbl(JsonNode node, String field) {
        JsonNode v = node.path(field).path("raw");
        return v.isMissingNode() || v.isNull() ? null : v.asDouble();
    }

    private Long nullableLong(JsonNode node, String field) {
        JsonNode v = node.path(field).path("raw");
        return v.isMissingNode() || v.isNull() ? null : v.asLong();
    }
}
