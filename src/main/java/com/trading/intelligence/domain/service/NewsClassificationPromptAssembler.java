package com.trading.intelligence.domain.service;

import com.trading.intelligence.domain.model.RawNewsArticle;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.llm.LlmRequest;

/**
 * Pure domain service: builds the LLM prompts that turn a raw article into a
 * categorised, sentiment-tagged record.
 *
 * Two modes:
 * <ul>
 *   <li>{@link #forTicker} — the article was fetched for a known ticker; the LLM
 *       judges whether it is good/bad FOR that stock and gives a one-line take.</li>
 *   <li>{@link #forMacro} — a market-wide article; the LLM categorises it and
 *       reads its impact on the broad market ({@code ALL}).</li>
 * </ul>
 * Both ask for compact JSON so the result is cheap to store as a summary.
 */
public class NewsClassificationPromptAssembler {

    private static final String TICKER_SYSTEM = """
            You analyse a single news article in the context of ONE stock ticker.
            Decide the category, whether the news is good or bad FOR THAT STOCK, and
            give a short one-sentence interpretation (not a neutral recap).

            category must be one of:
            MACRO_FED, MACRO_POLITICAL, MACRO_ECONOMIC, SECTOR, STOCK, OTHER
            sentiment must be one of: POSITIVE, NEGATIVE, NEUTRAL
            marketWide = true only if this is broad macro news affecting the whole market.

            Respond ONLY with valid JSON (no markdown):
            {"category":"...","sentiment":"...","summary":"<=200 chars","interpretation":"<=200 chars","marketWide":true|false}
            """;

    private static final String MACRO_SYSTEM = """
            You analyse a market-wide (macro) news article: Federal Reserve, monetary
            policy, the economy, or political/government statements that move US equities.
            Categorise it and read its impact on the BROAD MARKET.

            category must be one of:
            MACRO_FED, MACRO_POLITICAL, MACRO_ECONOMIC, SECTOR, OTHER
            sentiment must be one of: POSITIVE, NEGATIVE, NEUTRAL (for the overall market)

            Respond ONLY with valid JSON (no markdown):
            {"category":"...","sentiment":"...","summary":"<=200 chars","interpretation":"<=200 chars"}
            """;

    public LlmRequest forTicker(RawNewsArticle article, String ticker) {
        String user = """
                ## Ticker: %s

                ## Article
                Headline: %s
                Source: %s
                Content: %s
                """.formatted(ticker, article.headline(), article.source(), trim(article.content()));
        return LlmRequest.of(TICKER_SYSTEM, user, AnalysisRequestType.NEWS_SUMMARY);
    }

    public LlmRequest forMacro(RawNewsArticle article) {
        String user = """
                ## Macro Article
                Headline: %s
                Source: %s
                Content: %s
                """.formatted(article.headline(), article.source(), trim(article.content()));
        return LlmRequest.of(MACRO_SYSTEM, user, AnalysisRequestType.NEWS_SUMMARY);
    }

    private String trim(String content) {
        if (content == null) {
            return "(none)";
        }
        return content.length() > 1500 ? content.substring(0, 1500) : content;
    }
}
