package com.trading.scheduler.domain.job;

import com.trading.portfolio.domain.port.in.PortfolioUseCase;
import com.trading.scheduler.domain.model.JobExecutionLog;
import com.trading.scheduler.domain.port.out.JobExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioAnalysisJob {

    private final PortfolioUseCase portfolioUseCase;
    private final JobExecutionLogRepository logRepository;

    @Scheduled(cron = "${scheduler.portfolio.cron:0 0 0,5,11,15,19,23 * * *}")
    public void run() {
        log.info("PortfolioAnalysisJob starting...");
        JobExecutionLog execLog = JobExecutionLog.start("portfolio-analysis");
        logRepository.save(execLog);

        try {
            var positions = portfolioUseCase.listActivePositions();
            positions.forEach(pos -> portfolioUseCase.requestAnalysis(pos.getTicker()));
            execLog.complete(positions.size(), positions.size());
            log.info("PortfolioAnalysisJob complete — {} positions queued", positions.size());
        } catch (Exception e) {
            execLog.fail(e.getMessage());
            log.error("PortfolioAnalysisJob failed", e);
        } finally {
            logRepository.save(execLog);
        }
    }
}
