package com.trading.discovery.domain;

import com.trading.discovery.domain.model.DiscoveryCandidate;
import com.trading.discovery.domain.model.DiscoveryFilter;
import com.trading.discovery.domain.model.DiscoveryStatus;
import com.trading.discovery.domain.model.PotentialStock;
import com.trading.discovery.domain.model.SavedFilter;
import com.trading.discovery.domain.port.in.DiscoveryUseCase;
import com.trading.discovery.domain.port.out.DiscoveryCandidateRepository;
import com.trading.discovery.domain.port.out.DismissedTickerRepository;
import com.trading.discovery.domain.port.out.SavedFilterRepository;
import com.trading.discovery.domain.port.out.StockScreenerPort;
import com.trading.discovery.domain.service.DiscoveryEvaluationPromptAssembler;
import com.trading.marketdata.domain.model.TechnicalIndicatorSnapshot;
import com.trading.marketdata.domain.port.in.TechnicalAnalysisUseCase;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.event.AddToWatchlistRecommended;
import com.trading.shared.kernel.llm.LlmPort;
import com.trading.shared.kernel.llm.LlmRequest;
import com.trading.shared.kernel.llm.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application service for the discovery module.
 *
 * Pipeline per cycle:
 *  1. Screen the market (Finviz) using the saved/default filter.
 *  2. Skip tickers on the permanent dismiss blocklist.
 *  3. For each remaining candidate (capped), compute its technical indicators
 *     (marketdata) and ask the LLM — on TECHNICAL grounds only — whether to
 *     recommend it. Persist the verdict.
 *  4. The user later promotes (→ watchlist) or dismisses (→ blocklist) each one.
 */
@Slf4j
@Service
@Transactional
public class DiscoveryApplicationService implements DiscoveryUseCase {

    private static final int MAX_EVALUATIONS = 25;
    private static final int LOOKBACK_DAYS = 300;

    private final StockScreenerPort screenerPort;
    private final DiscoveryCandidateRepository candidateRepository;
    private final SavedFilterRepository filterRepository;
    private final DismissedTickerRepository dismissedRepository;
    private final TechnicalAnalysisUseCase technicalAnalysis;
    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final DiscoveryEvaluationPromptAssembler promptAssembler = new DiscoveryEvaluationPromptAssembler();

    public DiscoveryApplicationService(
            StockScreenerPort screenerPort,
            DiscoveryCandidateRepository candidateRepository,
            SavedFilterRepository filterRepository,
            DismissedTickerRepository dismissedRepository,
            TechnicalAnalysisUseCase technicalAnalysis,
            LlmPort llmPort,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher) {
        this.screenerPort = screenerPort;
        this.candidateRepository = candidateRepository;
        this.filterRepository = filterRepository;
        this.dismissedRepository = dismissedRepository;
        this.technicalAnalysis = technicalAnalysis;
        this.llmPort = llmPort;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    // -----------------------------------------------------------------------
    // Discovery cycle
    // -----------------------------------------------------------------------

    @Override
    public List<DiscoveryCandidate> runDiscoveryCycle(DiscoveryFilter filter) {
        log.info("Discovery cycle starting with filter: {}", filter);
        List<PotentialStock> screened = screenerPort.screen(filter);

        // Return ONLY the candidates screened+evaluated in THIS cycle so the caller
        // (e.g. ad-hoc UI run) sees exactly the tickers this filter matched on Finviz —
        // never the whole accumulated RECOMMENDED table from earlier, unrelated runs.
        List<DiscoveryCandidate> cycleResults = new java.util.ArrayList<>();
        int evaluated = 0;
        int suppressed = 0;
        for (PotentialStock stock : screened) {
            String ticker = stock.ticker().value();
            // Skip anything currently suppressed (user-dismissed OR parked as non up-trend):
            // it was already analysed recently, so re-running the LLM would just burn cost.
            if (dismissedRepository.isSuppressed(ticker)) {
                continue;
            }
            if (evaluated >= MAX_EVALUATIONS) {
                break;
            }
            DiscoveryCandidate candidate =
                    evaluateAndPersist(ticker, stock.screenerSource(), stock.matchedCriteria());
            evaluated++;
            // Only STRONG_UPTREND / UPTREND recommendations reach the UI. Everything else is
            // still persisted (audit) but parked with a cooldown so we don't re-analyse it daily.
            if (isPresentable(candidate)) {
                cycleResults.add(candidate);
            } else {
                dismissedRepository.suppress(ticker,
                        "Not an up-trend (trend=" + candidate.trendDirection() + ")");
                suppressed++;
            }
        }

        log.info("Discovery cycle complete — screened {}, evaluated {}, presented {}, parked {}",
                screened.size(), evaluated, cycleResults.size(), suppressed);
        return cycleResults;
    }

    @Override
    public List<DiscoveryCandidate> runActiveSavedFilters() {
        List<SavedFilter> active = filterRepository.findAllActive();
        if (active.isEmpty()) {
            return runDiscoveryCycle(DiscoveryFilter.defaults());
        }
        active.forEach(f -> runDiscoveryCycle(f.criteria()));
        return listRecommendations();
    }

    @Override
    public DiscoveryCandidate evaluateTicker(String ticker) {
        String symbol = ticker.toUpperCase();
        return evaluateAndPersist(symbol, "manual", Map.of("ticker", symbol));
    }

    private DiscoveryCandidate evaluateAndPersist(String ticker, String source, Map<String, String> criteria) {
        String company = criteria.getOrDefault("company", ticker);
        String sector = criteria.getOrDefault("sector", "Unknown");

        DiscoveryCandidate candidate = candidateRepository.findByTicker(ticker)
                .orElseGet(() -> DiscoveryCandidate.screened(ticker, company, sector, source, criteria));

        try {
            TechnicalIndicatorSnapshot snapshot = technicalAnalysis.computeIndicators(ticker, LOOKBACK_DAYS);
            LlmRequest request = promptAssembler.assemble(ticker, criteria, snapshot);
            LlmResponse response = llmPort.complete(request);
            Verdict v = parseVerdict(response.content());
            candidate = candidate.withEvaluation(v.recommend(), v.confidence(), v.reasoning(), v.trend());
            log.info("Discovery eval {}: recommend={} ({}) trend={}",
                    ticker, v.recommend(), v.confidence(), v.trend());
        } catch (Exception e) {
            log.warn("Discovery eval failed for {} — marking NOT_RECOMMENDED: {}", ticker, e.getMessage());
            candidate = candidate.withEvaluation(false, 0.0,
                    "Could not evaluate (no price data or LLM error): " + e.getMessage(), null);
        }
        return candidateRepository.save(candidate);
    }

    // -----------------------------------------------------------------------
    // Recommendations + user actions
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<DiscoveryCandidate> listRecommendations() {
        return candidateRepository.findByStatus(DiscoveryStatus.RECOMMENDED).stream()
                .filter(DiscoveryApplicationService::isPresentable)
                .filter(c -> !dismissedRepository.isSuppressed(c.ticker()))
                .toList();
    }

    @Override
    public void promoteTicker(String ticker) {
        String symbol = ticker.toUpperCase();
        candidateRepository.findByTicker(symbol).ifPresent(c ->
                candidateRepository.save(c.withStatus(DiscoveryStatus.PROMOTED)));
        eventPublisher.publishEvent(AddToWatchlistRecommended.of(
                new Ticker(symbol), "Promoted from discovery", 0.7));
        log.info("Ticker {} promoted from discovery to watchlist", symbol);
    }

    @Override
    public void dismissTicker(String ticker) {
        String symbol = ticker.toUpperCase();
        candidateRepository.findByTicker(symbol).ifPresent(c ->
                candidateRepository.save(c.withStatus(DiscoveryStatus.DISMISSED)));
        dismissedRepository.dismiss(symbol, "User dismissed from discovery");
        log.info("Ticker {} dismissed and blocklisted", symbol);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listDismissed() {
        return dismissedRepository.findAllTickers();
    }

    @Override
    public void undismissTicker(String ticker) {
        dismissedRepository.remove(ticker.toUpperCase());
    }

    // -----------------------------------------------------------------------
    // Saved filters
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<SavedFilter> listFilters() {
        return filterRepository.findAll();
    }

    @Override
    public SavedFilter saveFilter(String name, String description, DiscoveryFilter criteria) {
        return filterRepository.save(SavedFilter.create(name, description, criteria));
    }

    @Override
    public void activateFilter(UUID id) {
        filterRepository.findById(id).ifPresent(f -> filterRepository.save(f.withActive(true)));
    }

    @Override
    public void deactivateFilter(UUID id) {
        filterRepository.findById(id).ifPresent(f -> filterRepository.save(f.withActive(false)));
    }

    @Override
    public void deleteFilter(UUID id) {
        filterRepository.deleteById(id);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Verdict parseVerdict(String raw) {
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            String json = (start >= 0 && end > start) ? raw.substring(start, end + 1) : raw;
            JsonNode node = objectMapper.readTree(json);
            boolean recommend = node.path("recommend").asBoolean(false);
            double confidence = Math.max(0.0, Math.min(1.0, node.path("confidence").asDouble(0.5)));
            String reasoning = node.path("reasoning").asString("");
            String trend = node.path("trend").asString(null);
            return new Verdict(recommend, confidence, reasoning, trend);
        } catch (Exception e) {
            log.warn("Failed to parse discovery verdict, defaulting to not-recommended. Raw: {}", raw);
            return new Verdict(false, 0.0, "Unparseable LLM response: " + raw, null);
        }
    }

    private record Verdict(boolean recommend, double confidence, String reasoning, String trend) {}

    /** Only STRONG_UPTREND / UPTREND recommendations are surfaced to the user. */
    static boolean isPresentable(DiscoveryCandidate c) {
        return c != null && c.recommended() && isUptrend(c.trendDirection());
    }

    static boolean isUptrend(String trend) {
        if (trend == null) {
            return false;
        }
        String t = trend.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return t.equals("STRONG_UPTREND") || t.equals("UPTREND");
    }
}
