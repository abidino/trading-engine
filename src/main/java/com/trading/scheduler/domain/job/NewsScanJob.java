package com.trading.scheduler.domain.job;

import com.trading.intelligence.domain.IntelligenceApplicationService;
import com.trading.portfolio.domain.port.in.PortfolioUseCase;
import com.trading.scheduler.domain.model.JobExecutionLog;
import com.trading.scheduler.domain.port.out.JobExecutionLogRepository;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.event.AnalysisRequested;
import com.trading.watchlist.domain.port.in.WatchlistUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Scheduled job: scans macro market news plus per-ticker news for every held or
 * watched ticker, classifying and sentiment-tagging each article via the LLM.
 * When fresh articles arrive for a ticker it also fires an {@link AnalysisRequested}
 * event so the analysis pipeline re-evaluates that ticker on news (subject to the
 * orchestration cooldown), instead of waiting for the next scheduled sweep.
 *
 * Default cron {@code 0 0 * * * *} = hourly. Override via {@code scheduler.news.cron}.
 */
@Slf4j
@Component
public class NewsScanJob {

    private final IntelligenceApplicationService intelligenceService;
    private final PortfolioUseCase portfolioUseCase;
    private final WatchlistUseCase watchlistUseCase;
    private final JobExecutionLogRepository logRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final int perTickerLimit;
    private final int macroLimit;
    private final int triggerThreshold;

    public NewsScanJob(
            IntelligenceApplicationService intelligenceService,
            PortfolioUseCase portfolioUseCase,
            WatchlistUseCase watchlistUseCase,
            JobExecutionLogRepository logRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${scheduler.news.per-ticker-limit:15}") int perTickerLimit,
            @Value("${scheduler.news.macro-limit:30}") int macroLimit,
            @Value("${scheduler.news.trigger-threshold:1}") int triggerThreshold) {
        this.intelligenceService = intelligenceService;
        this.portfolioUseCase = portfolioUseCase;
        this.watchlistUseCase = watchlistUseCase;
        this.logRepository = logRepository;
        this.eventPublisher = eventPublisher;
        this.perTickerLimit = perTickerLimit;
        this.macroLimit = macroLimit;
        this.triggerThreshold = triggerThreshold;
    }

    @Scheduled(cron = "${scheduler.news.cron:0 0 0,4,5,10,11,12,13,14,15,16,17,18,19,20,21,22,23 * * *}")
    public void run() {
        log.info("NewsScanJob starting...");
        JobExecutionLog execLog = JobExecutionLog.start("news-scan");
        logRepository.save(execLog);

        Set<String> portfolioTickers = portfolioTickers();
        Set<String> tickers = collectTickers();
        int newArticles = 0;
        try {
            newArticles += intelligenceService.scanMacroNews(macroLimit);
            for (String ticker : tickers) {
                try {
                    int fresh = intelligenceService.scanTickerNews(ticker, perTickerLimit);
                    newArticles += fresh;
                    if (fresh >= triggerThreshold) {
                        AnalysisRequestType type = portfolioTickers.contains(ticker)
                                ? AnalysisRequestType.PORTFOLIO_REVIEW
                                : AnalysisRequestType.WATCHLIST_REVIEW;
                        eventPublisher.publishEvent(AnalysisRequested.of(new Ticker(ticker), type));
                        log.info("News trigger: {} fresh articles for {} → {} analysis requested",
                                fresh, ticker, type);
                    }
                } catch (Exception e) {
                    log.error("NewsScanJob failed for {}: {}", ticker, e.getMessage());
                }
            }
            execLog.complete(newArticles, tickers.size() + 1);
            log.info("NewsScanJob complete — {} new articles across {} tickers + macro",
                    newArticles, tickers.size());
        } catch (Exception e) {
            execLog.fail(e.getMessage());
            log.error("NewsScanJob failed", e);
        } finally {
            logRepository.save(execLog);
        }
    }

    private Set<String> portfolioTickers() {
        Set<String> tickers = new LinkedHashSet<>();
        portfolioUseCase.listActivePositions().forEach(p -> tickers.add(p.getTicker().value()));
        return tickers;
    }

    /** Distinct, order-preserving union of portfolio + watchlist tickers. */
    private Set<String> collectTickers() {
        Set<String> tickers = new LinkedHashSet<>();
        portfolioUseCase.listActivePositions()
                .forEach(p -> tickers.add(p.getTicker().value()));
        watchlistUseCase.listAll()
                .forEach(w -> tickers.add(w.getTicker().value()));
        return tickers;
    }
}
