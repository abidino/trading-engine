package com.trading.scheduler.domain.job;

import com.trading.marketdata.domain.model.IntradayQuote;
import com.trading.marketdata.domain.model.SupportResistanceLevels;
import com.trading.marketdata.domain.port.in.IntradayQuoteUseCase;
import com.trading.marketdata.domain.port.in.TechnicalAnalysisUseCase;
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
 * Scheduled job: refreshes the live intraday quote (pre-market / regular / post)
 * for every held or watched ticker and persists it. Kept separate from the daily
 * OHLC sync.
 *
 * <p>After refreshing, it compares the live price to the ticker's nearest support
 * and resistance; when price is within a small band of either level it fires an
 * {@link AnalysisRequested} event so the pipeline re-evaluates the ticker at the
 * moment it becomes actionable (subject to the orchestration cooldown).</p>
 *
 * Default cron {@code 0 0/10 * * * *} = every 10 minutes. Override via
 * {@code scheduler.intraday.cron}.
 */
@Slf4j
@Component
public class IntradayQuoteJob {

    private final IntradayQuoteUseCase intradayQuotes;
    private final TechnicalAnalysisUseCase technicalAnalysis;
    private final PortfolioUseCase portfolioUseCase;
    private final WatchlistUseCase watchlistUseCase;
    private final JobExecutionLogRepository logRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final double proximityBandPct;
    private final int lookbackDays;

    public IntradayQuoteJob(
            IntradayQuoteUseCase intradayQuotes,
            TechnicalAnalysisUseCase technicalAnalysis,
            PortfolioUseCase portfolioUseCase,
            WatchlistUseCase watchlistUseCase,
            JobExecutionLogRepository logRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${scheduler.intraday.sr-proximity-pct:1.0}") double proximityBandPct,
            @Value("${scheduler.intraday.sr-lookback-days:180}") int lookbackDays) {
        this.intradayQuotes = intradayQuotes;
        this.technicalAnalysis = technicalAnalysis;
        this.portfolioUseCase = portfolioUseCase;
        this.watchlistUseCase = watchlistUseCase;
        this.logRepository = logRepository;
        this.eventPublisher = eventPublisher;
        this.proximityBandPct = proximityBandPct;
        this.lookbackDays = lookbackDays;
    }

    @Scheduled(cron = "${scheduler.intraday.cron:0 0/10 * * * *}")
    public void run() {
        log.info("IntradayQuoteJob starting...");
        JobExecutionLog execLog = JobExecutionLog.start("intraday-quote");
        logRepository.save(execLog);

        Set<String> portfolioTickers = portfolioTickers();
        Set<String> tickers = collectTickers();
        int refreshed = 0;
        try {
            for (String ticker : tickers) {
                try {
                    IntradayQuote quote = intradayQuotes.refreshQuote(ticker);
                    refreshed++;
                    maybeTriggerOnLevelTouch(ticker, quote, portfolioTickers.contains(ticker));
                } catch (Exception e) {
                    log.warn("IntradayQuoteJob failed for {}: {}", ticker, e.getMessage());
                }
            }
            if (refreshed == tickers.size()) {
                execLog.complete(refreshed, tickers.size());
            } else {
                execLog.partialFailure(refreshed, tickers.size(),
                        (tickers.size() - refreshed) + " quotes failed");
            }
            log.info("IntradayQuoteJob complete — {}/{} quotes refreshed", refreshed, tickers.size());
        } catch (Exception e) {
            execLog.fail(e.getMessage());
            log.error("IntradayQuoteJob failed", e);
        } finally {
            logRepository.save(execLog);
        }
    }

    /** Fires an analysis request when the live price is within the band of nearest S/R. */
    private void maybeTriggerOnLevelTouch(String ticker, IntradayQuote quote, boolean held) {
        if (quote == null || quote.price() == null) {
            return;
        }
        double price = quote.price().doubleValue();
        if (price <= 0) {
            return;
        }
        try {
            SupportResistanceLevels sr = technicalAnalysis.supportResistance(ticker, lookbackDays);
            boolean nearSupport = withinBand(price, sr.nearestSupport());
            boolean nearResistance = withinBand(price, sr.nearestResistance());
            if (nearSupport || nearResistance) {
                AnalysisRequestType type = held
                        ? AnalysisRequestType.PORTFOLIO_REVIEW
                        : AnalysisRequestType.WATCHLIST_REVIEW;
                eventPublisher.publishEvent(AnalysisRequested.of(new Ticker(ticker), type));
                log.info("Price trigger: {} @ {} near {} → {} analysis requested",
                        ticker, price, nearSupport ? "support " + sr.nearestSupport()
                                : "resistance " + sr.nearestResistance(), type);
            }
        } catch (Exception e) {
            log.debug("S/R proximity check skipped for {}: {}", ticker, e.getMessage());
        }
    }

    private boolean withinBand(double price, Double level) {
        if (level == null || level <= 0) {
            return false;
        }
        double distancePct = Math.abs(price - level) / level * 100.0;
        return distancePct <= proximityBandPct;
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
