package com.trading.orchestration.domain;

import com.trading.orchestration.domain.model.AnalysisRequest;
import com.trading.orchestration.domain.model.AnalysisResult;
import com.trading.orchestration.domain.port.in.RequestAnalysisUseCase;
import com.trading.orchestration.domain.port.out.*;
import com.trading.shared.kernel.llm.LlmPort;
import com.trading.orchestration.domain.service.AnalysisOrchestrationDomainService;
import com.trading.orchestration.domain.service.PromptAssemblyService;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.event.AddToWatchlistRecommended;
import com.trading.shared.kernel.event.AnalysisRequested;
import com.trading.shared.kernel.event.DecisionProduced;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Application service: bridges Spring events ↔ domain orchestration.
 *
 * Responsibilities:
 * - Listens to AnalysisRequested events (published by scheduler / portfolio / watchlist / discovery)
 * - Delegates to AnalysisOrchestrationDomainService (pure domain logic)
 * - Publishes DecisionProduced event (consumed by decision-log and notification)
 * - Publishes AddToWatchlistRecommended when action is ADD_TO_WATCHLIST
 * - Exposes RequestAnalysisUseCase for direct REST-driven calls
 */
@Slf4j
@Service
public class AnalysisApplicationService implements RequestAnalysisUseCase {

    private final AnalysisOrchestrationDomainService domainService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalysisTriggerCoordinator triggerCoordinator;

    @org.springframework.beans.factory.annotation.Value("${orchestration.analysis.cooldown-minutes:90}")
    private long cooldownMinutes;

    public AnalysisApplicationService(
            LlmPort llmPort,
            TechnicalDataPort technicalDataPort,
            FundamentalDataPort fundamentalDataPort,
            NewsPort newsPort,
            SocialSignalPort socialSignalPort,
            TechnicalTrendPort technicalTrendPort,
            SupportResistancePort supportResistancePort,
            LivePricePort livePricePort,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            AnalysisTriggerCoordinator triggerCoordinator) {
        this.domainService = new AnalysisOrchestrationDomainService(
                llmPort, technicalDataPort, fundamentalDataPort,
                newsPort, socialSignalPort, technicalTrendPort, supportResistancePort,
                livePricePort, new PromptAssemblyService(), objectMapper);
        this.eventPublisher = eventPublisher;
        this.triggerCoordinator = triggerCoordinator;
    }

    // -----------------------------------------------------------------------
    // Event listener — consumes requests from scheduler / domain events
    // -----------------------------------------------------------------------

    /**
     * Runs on the dedicated {@code analysisExecutor} pool so a slow LLM call never blocks the
     * scheduler thread or the request thread that published the event.
     *
     * <p>Two guards keep the pipeline calm under many event-driven triggers (sweep, news, price):
     * a <b>cooldown</b> skips a ticker analysed very recently, and an <b>in-flight guard</b> skips
     * a ticker already being analysed. Manual UI triggers call {@link #analyze} directly and
     * bypass both guards, so an explicit user request always runs.</p>
     */
    @Async("analysisExecutor")
    @EventListener
    public void onAnalysisRequested(AnalysisRequested event) {
        String ticker = event.ticker().value();
        if (!triggerCoordinator.cooldownElapsed(ticker, java.time.Duration.ofMinutes(cooldownMinutes))) {
            log.info("Analysis for {} within {}-min cooldown — skipping {} trigger",
                    ticker, cooldownMinutes, event.requestType());
            return;
        }
        if (!triggerCoordinator.beginIfAbsent(ticker)) {
            log.info("Analysis already in-flight for {} — skipping duplicate {} request",
                    ticker, event.requestType());
            return;
        }
        try {
            log.info("AnalysisRequested received: ticker={} type={}", ticker, event.requestType());
            AnalysisRequest request = new AnalysisRequest(
                    event.ticker(), event.requestType(), event.contextMetadata());
            analyze(request);
        } finally {
            triggerCoordinator.end(ticker);
        }
    }

    // -----------------------------------------------------------------------
    // Inbound port — called directly from REST controller
    // -----------------------------------------------------------------------

    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        log.info("Starting analysis: ticker={} type={}", request.ticker(), request.requestType());

        AnalysisResult result = domainService.analyze(request);

        log.info("Analysis done: ticker={} action={} confidence={}",
                request.ticker(), result.action(), result.confidence());

        // Publish DecisionProduced — consumed by decision-log and notification
        eventPublisher.publishEvent(new DecisionProduced(
                request.ticker(),
                request.requestType(),
                result.action().name(),
                result.confidence(),
                result.reasoning(),
                result.technicalSummary(),
                result.fundamentalSummary(),
                result.newsSummary(),
                result.socialSummary(),
                result.counterThesis(),
                result.keyRisks(),
                result.levels(),
                result.decidedAt()
        ));

        // If discovery analysis recommends watchlisting, emit secondary event
        if (request.requestType() == AnalysisRequestType.DISCOVERY
                && "ADD_TO_WATCHLIST".equals(result.action().name())) {
            eventPublisher.publishEvent(AddToWatchlistRecommended.of(
                    request.ticker(), result.reasoning(), result.confidence()));
        }

        return result;
    }
}
