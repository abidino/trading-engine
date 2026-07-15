package com.trading.scheduler.domain.job;

import com.trading.discovery.domain.port.in.DiscoveryUseCase;
import com.trading.scheduler.domain.model.JobExecutionLog;
import com.trading.scheduler.domain.port.out.JobExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscoveryJob {

    private final DiscoveryUseCase discoveryUseCase;
    private final JobExecutionLogRepository logRepository;

    /** Runs all active saved filters (screen + LLM evaluation). Cron via application.yml. */
    @Scheduled(cron = "${scheduler.discovery.cron:0 0 5 * * MON-FRI}")
    public void run() {
        log.info("DiscoveryJob starting...");
        JobExecutionLog execLog = JobExecutionLog.start("discovery");
        logRepository.save(execLog);

        try {
            var recommendations = discoveryUseCase.runActiveSavedFilters();
            execLog.complete(recommendations.size(), recommendations.size());
            log.info("DiscoveryJob complete — {} recommendations produced", recommendations.size());
        } catch (Exception e) {
            execLog.fail(e.getMessage());
            log.error("DiscoveryJob failed", e);
        } finally {
            logRepository.save(execLog);
        }
    }
}
