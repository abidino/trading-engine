package com.trading.scheduler.domain.job;

import com.trading.scheduler.domain.model.JobExecutionLog;
import com.trading.scheduler.domain.port.out.JobExecutionLogRepository;
import com.trading.watchlist.domain.port.in.WatchlistUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchlistAnalysisJob {

    private final WatchlistUseCase watchlistUseCase;
    private final JobExecutionLogRepository logRepository;

    @Scheduled(cron = "${scheduler.watchlist.cron:0 0 0,5,11,15,19,23 * * *}")
    public void run() {
        log.info("WatchlistAnalysisJob starting...");
        JobExecutionLog execLog = JobExecutionLog.start("watchlist-analysis");
        logRepository.save(execLog);

        try {
            var items = watchlistUseCase.listAll().stream().filter(i -> i.isApproved()).toList();
            items.forEach(item -> watchlistUseCase.requestAnalysis(item.getTicker()));
            execLog.complete(items.size(), items.size());
            log.info("WatchlistAnalysisJob complete — {} items queued", items.size());
        } catch (Exception e) {
            execLog.fail(e.getMessage());
            log.error("WatchlistAnalysisJob failed", e);
        } finally {
            logRepository.save(execLog);
        }
    }
}
