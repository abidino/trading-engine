package com.trading.intelligence.domain;

import com.trading.intelligence.domain.model.NewsArticle;
import com.trading.intelligence.domain.model.NewsCategory;
import com.trading.intelligence.domain.model.NewsSentiment;
import com.trading.intelligence.domain.model.NewsTag;
import com.trading.intelligence.domain.model.RawNewsArticle;
import com.trading.intelligence.domain.model.SocialSignal;
import com.trading.intelligence.domain.port.out.NewsProviderPort;
import com.trading.intelligence.domain.port.out.NewsRepository;
import com.trading.intelligence.domain.port.out.SocialSignalProviderPort;
import com.trading.intelligence.domain.port.out.SocialSignalRepository;
import com.trading.intelligence.domain.service.NewsClassificationPromptAssembler;
import com.trading.shared.kernel.llm.LlmPort;
import com.trading.shared.kernel.llm.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects, categorises and sentiment-tags news + social signals.
 *
 * News flow: fetch raw articles → skip already-seen URLs → ask the LLM to
 * categorise and read sentiment (good/bad for the ticker, or market-wide for
 * macro) → persist only the link + short summary + tags. Orchestration later
 * reads these via its own outbound ports.
 */
@Slf4j
@Service
@Transactional
public class IntelligenceApplicationService {

    private final NewsProviderPort newsProvider;
    private final SocialSignalProviderPort socialProvider;
    private final NewsRepository newsRepository;
    private final SocialSignalRepository socialRepository;
    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;

    private final NewsClassificationPromptAssembler promptAssembler = new NewsClassificationPromptAssembler();

    public IntelligenceApplicationService(
            NewsProviderPort newsProvider,
            SocialSignalProviderPort socialProvider,
            NewsRepository newsRepository,
            SocialSignalRepository socialRepository,
            LlmPort llmPort,
            ObjectMapper objectMapper) {
        this.newsProvider = newsProvider;
        this.socialProvider = socialProvider;
        this.newsRepository = newsRepository;
        this.socialRepository = socialRepository;
        this.llmPort = llmPort;
        this.objectMapper = objectMapper;
    }

    // -----------------------------------------------------------------------
    // News — scanning
    // -----------------------------------------------------------------------

    /** Fetch + classify + persist ticker-specific news. Returns the number newly stored. */
    public int scanTickerNews(String ticker, int limit) {
        String symbol = ticker.toUpperCase();
        List<RawNewsArticle> raw = newsProvider.fetchForTicker(symbol, limit);
        int saved = 0;
        for (RawNewsArticle article : raw) {
            if (isDuplicate(article)) {
                continue;
            }
            try {
                NewsArticle stored = classifyTicker(article, symbol);
                newsRepository.save(stored);
                saved++;
            } catch (Exception e) {
                log.warn("Failed to classify ticker news '{}': {}", article.url(), e.getMessage());
            }
        }
        log.info("Scanned ticker news {} — {} new of {} fetched", symbol, saved, raw.size());
        return saved;
    }

    /** Fetch + classify + persist macro market news (tagged ALL). Returns newly stored count. */
    public int scanMacroNews(int limit) {
        List<RawNewsArticle> raw = newsProvider.fetchMacroNews(limit);
        int saved = 0;
        for (RawNewsArticle article : raw) {
            if (isDuplicate(article)) {
                continue;
            }
            try {
                NewsArticle stored = classifyMacro(article);
                newsRepository.save(stored);
                saved++;
            } catch (Exception e) {
                log.warn("Failed to classify macro news '{}': {}", article.url(), e.getMessage());
            }
        }
        log.info("Scanned macro news — {} new of {} fetched", saved, raw.size());
        return saved;
    }

    private boolean isDuplicate(RawNewsArticle article) {
        return article.url() == null || article.url().isBlank()
                || newsRepository.findByUrl(article.url()).isPresent();
    }

    private NewsArticle classifyTicker(RawNewsArticle article, String ticker) {
        LlmResponse response = llmPort.complete(promptAssembler.forTicker(article, ticker));
        JsonNode node = readJson(response.content());

        NewsCategory category = parseCategory(node.path("category").asString("OTHER"));
        NewsSentiment sentiment = parseSentiment(node.path("sentiment").asString("NEUTRAL"));
        String summary = node.path("summary").asString("");
        String interpretation = node.path("interpretation").asString(summary);
        boolean marketWide = node.path("marketWide").asBoolean(false);

        List<NewsTag> tags = new ArrayList<>();
        tags.add(NewsTag.of(ticker, sentiment, interpretation));
        if (marketWide) {
            tags.add(NewsTag.of(NewsTag.ALL, sentiment, interpretation));
        }
        return NewsArticle.create(article.url(), article.headline(), article.source(),
                category, summary, article.publishedAt(), tags);
    }

    private NewsArticle classifyMacro(RawNewsArticle article) {
        LlmResponse response = llmPort.complete(promptAssembler.forMacro(article));
        JsonNode node = readJson(response.content());

        NewsCategory category = parseCategory(node.path("category").asString("MACRO_ECONOMIC"));
        NewsSentiment sentiment = parseSentiment(node.path("sentiment").asString("NEUTRAL"));
        String summary = node.path("summary").asString("");
        String interpretation = node.path("interpretation").asString(summary);

        List<NewsTag> tags = List.of(NewsTag.of(NewsTag.ALL, sentiment, interpretation));
        return NewsArticle.create(article.url(), article.headline(), article.source(),
                category, summary, article.publishedAt(), tags);
    }

    // -----------------------------------------------------------------------
    // News — queries
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NewsArticle> getNewsForTicker(String ticker, int sinceDays) {
        Instant since = Instant.now().minus(sinceDays, ChronoUnit.DAYS);
        return newsRepository.findForTickerSince(ticker.toUpperCase(), since);
    }

    @Transactional(readOnly = true)
    public List<NewsArticle> getRecentNews(int limit) {
        return newsRepository.findRecent(limit);
    }

    // -----------------------------------------------------------------------
    // Social (unchanged — structure only for now)
    // -----------------------------------------------------------------------

    public void collectSocialSignalsForTicker(String ticker, int limit) {
        List<SocialSignal> signals = socialProvider.fetchForTicker(ticker, limit);
        socialRepository.saveAll(signals);
        log.debug("Collected {} social signals for {}", signals.size(), ticker);
    }

    @Transactional(readOnly = true)
    public List<SocialSignal> getSocialSignalsByTicker(String ticker) {
        return socialRepository.findByTicker(ticker);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private JsonNode readJson(String raw) {
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            String json = (start >= 0 && end > start) ? raw.substring(start, end + 1) : raw;
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private NewsCategory parseCategory(String value) {
        try {
            return NewsCategory.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return NewsCategory.OTHER;
        }
    }

    private NewsSentiment parseSentiment(String value) {
        try {
            return NewsSentiment.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return NewsSentiment.NEUTRAL;
        }
    }
}
