package com.trading.discovery.infrastructure;

import com.trading.discovery.domain.model.DiscoveryFilter;
import com.trading.discovery.domain.model.PotentialStock;
import com.trading.discovery.domain.port.out.StockScreenerPort;
import com.trading.shared.kernel.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Screener backed by the <b>free</b> Finviz screener HTML (no subscription / auth token required).
 *
 * URL pattern: {@code https://finviz.com/screener.ashx?v=111&f={filters}&r={rowOffset}}
 * The {@code v=111} (Overview) view renders an HTML table whose rows contain:
 * {@code No, Ticker, Company, Sector, Industry, Country, Market Cap, P/E, Price, Change, Volume}.
 * We parse those rows directly (no API key needed). The free screener returns 20
 * rows per page, so we page through a bounded number of pages via the {@code r} offset.
 *
 * Finviz uses discrete preset thresholds for fundamental/technical filters, so
 * numeric criteria are snapped to the nearest supported preset. RSI is left to
 * our own technical-analysis stage (Finviz only offers one-sided RSI tokens).
 *
 * If a paid Finviz Elite token is ever supplied via {@code finviz.auth-token} it is
 * sent as the {@code auth_token} cookie (unlocks more rows / removes rate limits),
 * but it is entirely optional.
 */
@Slf4j
@Component
public class FinvizScreenerAdapter implements StockScreenerPort {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /** Free screener returns 20 rows/page; cap total pages to stay polite. */
    private static final int ROWS_PER_PAGE = 20;
    private final int maxPages;

    private final RestClient restClient;
    private final FinvizQueryMapper queryMapper;

    public FinvizScreenerAdapter(
            FinvizQueryMapper queryMapper,
            @Value("${finviz.base-url:https://finviz.com}") String baseUrl,
            @Value("${finviz.auth-token:}") String authToken,
            @Value("${finviz.max-pages:3}") int maxPages) {
        this.queryMapper = queryMapper;
        this.maxPages = Math.max(1, maxPages);

        // Finviz redirects finviz.com -> www.finviz.com; the JDK HttpClient must
        // follow redirects (Spring's default RestClient does NOT), otherwise we
        // receive an empty 301 body and parse zero tickers.
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("Accept", "text/html");
        if (!authToken.isBlank()) {
            builder.defaultHeader("Cookie", "auth_token=" + authToken);
        }
        this.restClient = builder.build();
    }

    @Override
    public List<PotentialStock> screen(DiscoveryFilter filter) {
        try {
            String filterParams = queryMapper.toFilterParam(filter);
            log.info("Finviz (free) screen with f={}", filterParams);

            Map<String, PotentialStock> bySymbol = new LinkedHashMap<>();
            for (int page = 0; page < maxPages; page++) {
                int rowOffset = page * ROWS_PER_PAGE + 1;
                String html = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/screener.ashx")
                                .queryParam("v", "111")
                                .queryParam("f", filterParams)
                                .queryParam("r", rowOffset)
                                .build())
                        .retrieve()
                        .body(String.class);

                List<PotentialStock> pageStocks = parseHtml(html);
                if (pageStocks.isEmpty()) {
                    break;
                }
                for (PotentialStock s : pageStocks) {
                    bySymbol.putIfAbsent(s.ticker().value(), s);
                }
                if (pageStocks.size() < ROWS_PER_PAGE) {
                    break;
                }
            }
            log.info("Finviz (free) screen returned {} unique tickers", bySymbol.size());
            return new ArrayList<>(bySymbol.values());
        } catch (Exception e) {
            log.warn("Finviz screen failed: {}", e.getMessage());
            return List.of();
        }
    }

    // -----------------------------------------------------------------------
    // HTML parsing (free screener, v=111 Overview table)
    // Row columns: No, Ticker, Company, Sector, Industry, Country,
    //              Market Cap, P/E, Price, Change, Volume
    // -----------------------------------------------------------------------

    private static final Pattern ROW_PATTERN = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL);
    private static final Pattern CELL_PATTERN = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern TICKER_LINK_PATTERN = Pattern.compile("(?:stock|quote)\\.ashx\\?t=[A-Z.]+|stock\\?t=[A-Z.]+");

    private List<PotentialStock> parseHtml(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        int tableStart = html.indexOf("styled-table-new");
        String scope = tableStart >= 0 ? html.substring(tableStart) : html;

        List<PotentialStock> results = new ArrayList<>();
        Matcher rowMatcher = ROW_PATTERN.matcher(scope);
        while (rowMatcher.find()) {
            String row = rowMatcher.group(1);
            if (!TICKER_LINK_PATTERN.matcher(row).find()) {
                continue;
            }
            List<String> cols = new ArrayList<>();
            Matcher cellMatcher = CELL_PATTERN.matcher(row);
            while (cellMatcher.find()) {
                cols.add(stripHtml(cellMatcher.group(1)));
            }
            // Expect: [No, Ticker, Company, Sector, Industry, Country, MarketCap, P/E, Price, Change, Volume]
            if (cols.size() < 9) {
                continue;
            }
            String symbol = cols.get(1);
            if (symbol.isEmpty() || !symbol.matches("[A-Z.]+")) {
                continue;
            }
            Map<String, String> criteria = new LinkedHashMap<>();
            putIfPresent(criteria, "company", cols, 2);
            putIfPresent(criteria, "sector", cols, 3);
            putIfPresent(criteria, "industry", cols, 4);
            putIfPresent(criteria, "country", cols, 5);
            putIfPresent(criteria, "marketCap", cols, 6);
            putIfPresent(criteria, "peRatio", cols, 7);
            putIfPresent(criteria, "price", cols, 8);
            results.add(PotentialStock.create(new Ticker(symbol), "finviz", criteria));
        }
        return results;
    }

    private String stripHtml(String s) {
        String noTags = TAG_PATTERN.matcher(s).replaceAll("");
        return noTags.replace("&amp;", "&").replace("&nbsp;", " ").trim();
    }

    private void putIfPresent(Map<String, String> map, String key, List<String> cols, int index) {
        if (index >= 0 && index < cols.size()) {
            String value = cols.get(index).trim();
            if (!value.isEmpty() && !"-".equals(value)) {
                map.put(key, value);
            }
        }
    }
}
