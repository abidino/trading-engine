package com.trading.orchestration.domain.service;

import com.trading.orchestration.domain.exception.AnalysisParsingException;
import com.trading.orchestration.domain.model.*;
import com.trading.orchestration.domain.port.out.*;
import com.trading.shared.kernel.llm.LlmPort;
import com.trading.shared.kernel.llm.LlmRequest;
import com.trading.shared.kernel.llm.LlmResponse;
import com.trading.shared.kernel.TradingLevels;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain service: orchestrates the full single-pass analysis pipeline.
 *
 * Flow:
 *  1. Fetch technical + fundamental data from outbound ports
 *  2. Fetch news + social signals
 *  3. Summarise each via a cheap LLM call
 *  4. Assemble AnalysisContext
 *  5. Call LLM for the final structured decision
 *  6. Parse JSON response into AnalysisResult
 *
 * Pure domain service — no Spring annotations, no IO frameworks.
 * All dependencies injected via constructor by AnalysisApplicationService.
 */
public class AnalysisOrchestrationDomainService {

    private final LlmPort llmPort;
    private final TechnicalDataPort technicalDataPort;
    private final FundamentalDataPort fundamentalDataPort;
    private final NewsPort newsPort;
    private final SocialSignalPort socialSignalPort;
    private final TechnicalTrendPort technicalTrendPort;
    private final SupportResistancePort supportResistancePort;
    private final PromptAssemblyService promptAssembly;
    private final ObjectMapper objectMapper;

    private static final int NEWS_LIMIT = 20;
    private static final int SOCIAL_LIMIT = 10;

    public AnalysisOrchestrationDomainService(
            LlmPort llmPort,
            TechnicalDataPort technicalDataPort,
            FundamentalDataPort fundamentalDataPort,
            NewsPort newsPort,
            SocialSignalPort socialSignalPort,
            TechnicalTrendPort technicalTrendPort,
            SupportResistancePort supportResistancePort,
            PromptAssemblyService promptAssembly,
            ObjectMapper objectMapper) {
        this.llmPort = llmPort;
        this.technicalDataPort = technicalDataPort;
        this.fundamentalDataPort = fundamentalDataPort;
        this.newsPort = newsPort;
        this.socialSignalPort = socialSignalPort;
        this.technicalTrendPort = technicalTrendPort;
        this.supportResistancePort = supportResistancePort;
        this.promptAssembly = promptAssembly;
        this.objectMapper = objectMapper;
    }

    public AnalysisResult analyze(AnalysisRequest request) {
        String ticker = request.ticker().value();

        // 0. Ensure the underlying data is present & reasonably fresh before reading it.
        //    Each adapter gathers from its provider only if the local cache is missing/stale.
        ensureDataReady(ticker);

        // 1. Gather data
        TechnicalData technical = technicalDataPort.fetchForTicker(ticker);
        FundamentalData fundamental = fundamentalDataPort.fetchForTicker(ticker);
        List<String> headlines = newsPort.fetchHeadlinesForTicker(ticker, NEWS_LIMIT);
        List<String> signals = socialSignalPort.fetchSignalsForTicker(ticker, SOCIAL_LIMIT);
        String trendSummary = technicalTrendPort.fetchTrendSummary(ticker);
        SupportResistancePort.Snapshot supportResistance = supportResistancePort.fetchForTicker(ticker);

        // 2. Summarise raw content via LLM (cheap, short calls)
        String newsSummary = headlines.isEmpty()
                ? "No recent news available."
                : summarise(headlines, "news");
        String socialSummary = signals.isEmpty()
                ? "No social signals available."
                : summarise(signals, "social media");

        // 3. Assemble context and call LLM for final decision
        AnalysisContext context = new AnalysisContext(
                request.ticker(), request.requestType(),
                technical, fundamental, newsSummary, socialSummary, trendSummary,
                supportResistance, buildPositionContext(request));

        LlmRequest llmRequest = promptAssembly.assembleAnalysisRequest(context);
        LlmResponse llmResponse = llmPort.complete(llmRequest);

        // 4. Parse structured JSON response, then overlay the authoritative computed S/R levels
        //    (nearest support/resistance are facts, not LLM opinions).
        AnalysisResult parsed = parseResult(llmResponse.content());
        return withComputedLevels(parsed, supportResistance);
    }

    /**
     * Gathers technical, news and social data for the ticker if the local cache is
     * missing or stale, so analyses never run against empty/outdated context.
     * Each adapter owns its own freshness policy; failures are logged, not fatal.
     */
    private void ensureDataReady(String ticker) {
        technicalDataPort.ensureFresh(ticker);
        newsPort.ensureFresh(ticker);
        socialSignalPort.ensureFresh(ticker);
    }

    /** Overrides the LLM's nearest support/resistance with the authoritative computed values. */
    private AnalysisResult withComputedLevels(AnalysisResult result, SupportResistancePort.Snapshot sr) {
        if (sr == null) {
            return result;
        }
        TradingLevels l = result.levels();
        TradingLevels merged = new TradingLevels(
                l.entryLow(), l.entryHigh(),
                l.aggressiveEntry(), l.idealEntry(), l.safeEntry(),
                l.stopLoss(), l.takeProfit(),
                sr.nearestSupport() != null ? sr.nearestSupport() : l.nearestSupport(),
                sr.nearestResistance() != null ? sr.nearestResistance() : l.nearestResistance());
        return new AnalysisResult(
                result.action(), result.confidence(), result.reasoning(),
                result.technicalSummary(), result.fundamentalSummary(),
                result.newsSummary(), result.socialSummary(),
                result.counterThesis(), result.keyRisks(),
                merged, result.decidedAt());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Renders position-specific metadata (entry price, size) into prompt context, if present. */
    private String buildPositionContext(AnalysisRequest request) {
        String entry = request.meta("entryPrice");
        String qty = request.meta("quantity");
        if (entry == null && qty == null) {
            return "No open position (evaluating as a fresh candidate).";
        }
        StringBuilder sb = new StringBuilder("Open position — ");
        if (entry != null) sb.append("average entry price: ").append(entry).append(". ");
        if (qty != null) sb.append("quantity held: ").append(qty).append(". ");
        return sb.toString().trim();
    }

    private String summarise(List<String> items, String type) {
        String raw = items.stream().collect(Collectors.joining("\n\n---\n\n"));
        LlmRequest req = promptAssembly.assembleSummarizationRequest(raw, type);
        return llmPort.complete(req).content();
    }

    private AnalysisResult parseResult(String rawResponse) {
        String json = sanitizeJson(rawResponse);
        try {
            JsonNode node = objectMapper.readTree(json);
            return new AnalysisResult(
                    AnalysisAction.valueOf(node.path("action").asString("").trim().toUpperCase()),
                    node.path("confidence").asDouble(0.5),
                    node.path("reasoning").asString(""),
                    node.path("technicalSummary").asString(""),
                    node.path("fundamentalSummary").asString(""),
                    node.path("newsSummary").asString(""),
                    node.path("socialSummary").asString(""),
                    node.path("counterThesis").asString(""),
                    parseStringList(node.path("keyRisks")),
                    parseLevels(node),
                    Instant.now()
            );
        } catch (Exception e) {
            throw new AnalysisParsingException(
                    "Failed to parse LLM response into AnalysisResult. Raw: " + rawResponse, e);
        }
    }

    /**
     * Extracts a parseable JSON object from a raw LLM response. Local models (e.g. phi4) frequently
     * wrap the JSON in a ```json ... ``` markdown fence or add prose around it despite instructions;
     * this strips fences and narrows to the outermost {@code { ... }} so the parser stays robust.
     */
    static String sanitizeJson(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        // Strip a leading ```json / ``` fence and any trailing ``` fence.
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline >= 0) {
                s = s.substring(firstNewline + 1);
            }
            int closingFence = s.lastIndexOf("```");
            if (closingFence >= 0) {
                s = s.substring(0, closingFence);
            }
            s = s.trim();
        }
        // Narrow to the outermost JSON object if there is surrounding prose.
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            s = s.substring(start, end + 1);
        }
        return s;
    }

    /** Extracts the actionable price levels from the LLM JSON, tolerating missing/blank fields. */
    static TradingLevels parseLevels(JsonNode node) {
        JsonNode levels = node.path("levels");
        JsonNode src = levels.isObject() ? levels : node; // accept flat or nested layout
        return new TradingLevels(
                num(src, "entryLow"),
                num(src, "entryHigh"),
                num(src, "aggressiveEntry"),
                num(src, "idealEntry"),
                num(src, "safeEntry"),
                num(src, "stopLoss"),
                num(src, "takeProfit"),
                num(src, "nearestSupport"),
                num(src, "nearestResistance"));
    }

    /**
     * Reads a list of strings from the LLM JSON, tolerating: a JSON array of strings, a single
     * string (wrapped into a one-element list), or a missing/blank/null node (empty list).
     * Blank entries are dropped and each is trimmed.
     */
    static List<String> parseStringList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return List.of();
        java.util.List<String> out = new java.util.ArrayList<>();
        if (node.isArray()) {
            for (JsonNode el : node) {
                String s = el.isValueNode() ? el.asString("").trim() : el.toString().trim();
                if (!s.isEmpty()) out.add(s);
            }
        } else {
            String s = node.asString("").trim();
            if (!s.isEmpty()) out.add(s);
        }
        return List.copyOf(out);
    }

    /** Reads a numeric field as a nullable Double; returns null when absent, blank, or non-numeric. */
    static Double num(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        if (v.isNumber()) return v.asDouble();
        String s = v.asString("").trim();
        if (s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
