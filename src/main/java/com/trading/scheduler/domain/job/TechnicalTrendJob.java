package com.trading.scheduler.domain.job;

import com.trading.marketdata.domain.port.in.TechnicalAnalysisUseCase;
import com.trading.portfolio.domain.port.in.PortfolioUseCase;
import com.trading.scheduler.domain.model.JobExecutionLog;
import com.trading.scheduler.domain.port.out.JobExecutionLogRepository;
import com.trading.watchlist.domain.port.in.WatchlistUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Daily job: after the US market closes, compute technical indicators and an LLM
 * trend verdict for every ticker that is either held in the portfolio or watched.
 *
 * Default cron {@code 0 30 21 * * MON-FRI} = 21:30 UTC (~16:30 US/Eastern, shortly
 * after the 16:00 close). Override via {@code scheduler.technical.cron}.
 */
@Slf4j
@Component
public class TechnicalTrendJob {

    private final PortfolioUseCase portfolioUseCase;
    private final WatchlistUseCase watchlistUseCase;
    private final TechnicalAnalysisUseCase technicalAnalysis;
    private final JobExecutionLogRepository logRepository;
    private final int lookbackDays;

    public TechnicalTrendJob(
            PortfolioUseCase portfolioUseCase,
            WatchlistUseCase watchlistUseCase,
            TechnicalAnalysisUseCase technicalAnalysis,
            JobExecutionLogRepository logRepository,
            @Value("${scheduler.technical.lookback-days:400}") int lookbackDays) {
        this.portfolioUseCase = portfolioUseCase;
        this.watchlistUseCase = watchlistUseCase;
        this.technicalAnalysis = technicalAnalysis;
        this.logRepository = logRepository;
        this.lookbackDays = lookbackDays;
    }

    @Scheduled(cron = "${scheduler.technical.cron:0 30 21 * * MON-FRI}")
    public void run() {
        log.info("TechnicalTrendJob starting...");
        JobExecutionLog execLog = JobExecutionLog.start("technical-trend");
        logRepository.save(execLog);

        Set<String> tickers = collectTickers();
        int processed = 0;
        try {
            for (String ticker : tickers) {
                try {
                    technicalAnalysis.analyzeTrend(ticker, lookbackDays);
                    processed++;
                } catch (Exception e) {
                    log.error("TechnicalTrendJob failed for {}: {}", ticker, e.getMessage());
                }
            }
            execLog.complete(processed, tickers.size());
            log.info("TechnicalTrendJob complete — {}/{} tickers analyzed", processed, tickers.size());
        } catch (Exception e) {
            execLog.fail(e.getMessage());
            log.error("TechnicalTrendJob failed", e);
        } finally {
            logRepository.save(execLog);
        }
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
