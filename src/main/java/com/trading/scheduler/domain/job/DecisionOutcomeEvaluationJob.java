package com.trading.scheduler.domain.job;

import com.trading.decisionlog.domain.DecisionLogApplicationService;
import com.trading.scheduler.domain.model.JobExecutionLog;
import com.trading.scheduler.domain.port.out.JobExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Closes the feedback loop: periodically evaluates still-PENDING AI decisions against
 * subsequent price action and records a VALIDATED/INVALIDATED outcome, so the decision
 * journal builds an honest, evidence-based hit-rate over time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionOutcomeEvaluationJob {

    private final DecisionLogApplicationService decisionLog;
    private final JobExecutionLogRepository logRepository;

    @Value("${decisions.evaluation.min-age-days:5}")
    private int minAgeDays;

    @Value("${decisions.evaluation.band-pct:3.0}")
    private double bandPct;

    @Scheduled(cron = "${decisions.evaluation.cron:0 30 1 * * *}")
    public void run() {
        log.info("DecisionOutcomeEvaluationJob starting (minAgeDays={}, band={}%)...", minAgeDays, bandPct);
        JobExecutionLog execLog = JobExecutionLog.start("decision-outcome-evaluation");
        logRepository.save(execLog);
        try {
            int resolved = decisionLog.evaluatePendingOutcomes(minAgeDays, bandPct);
            execLog.complete(resolved, resolved);
            log.info("DecisionOutcomeEvaluationJob complete — {} decisions resolved", resolved);
        } catch (Exception e) {
            execLog.fail(e.getMessage());
            log.error("DecisionOutcomeEvaluationJob failed", e);
        } finally {
            logRepository.save(execLog);
        }
    }
}
