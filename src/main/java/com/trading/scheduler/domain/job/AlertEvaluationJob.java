package com.trading.scheduler.domain.job;

import com.trading.notification.domain.AlertEvaluationService;
import com.trading.scheduler.domain.model.JobExecutionLog;
import com.trading.scheduler.domain.port.out.JobExecutionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Scheduled job: evaluates proactive threshold alerts (stop-loss / take-profit / entry-zone /
 * portfolio-drop) for watched + held tickers. Runs frequently but only fires each condition
 * once per day (de-duplicated in {@link AlertEvaluationService}).
 *
 * <p>Guarded to the US extended trading window (pre-market through post-market, Mon–Fri) so it
 * doesn't spend cycles overnight/weekends when prices don't move. Disable the guard with
 * {@code alerts.market-hours-only=false}. Default cron {@code 0 0/5 * * * *} = every 5 minutes.</p>
 */
@Slf4j
@Component
public class AlertEvaluationJob {

    private static final ZoneId US_EASTERN = ZoneId.of("America/New_York");
    private static final LocalTime SESSION_OPEN = LocalTime.of(4, 0);   // pre-market
    private static final LocalTime SESSION_CLOSE = LocalTime.of(20, 0); // post-market

    private final AlertEvaluationService alertEvaluationService;
    private final JobExecutionLogRepository logRepository;
    private final boolean enabled;
    private final boolean marketHoursOnly;

    public AlertEvaluationJob(
            AlertEvaluationService alertEvaluationService,
            JobExecutionLogRepository logRepository,
            @Value("${alerts.enabled:true}") boolean enabled,
            @Value("${alerts.market-hours-only:true}") boolean marketHoursOnly) {
        this.alertEvaluationService = alertEvaluationService;
        this.logRepository = logRepository;
        this.enabled = enabled;
        this.marketHoursOnly = marketHoursOnly;
    }

    @Scheduled(cron = "${alerts.cron:0 0/5 * * * *}")
    public void run() {
        if (!enabled) {
            return;
        }
        if (marketHoursOnly && !withinTradingWindow()) {
            log.debug("AlertEvaluationJob skipped — outside US trading window");
            return;
        }

        JobExecutionLog execLog = JobExecutionLog.start("alert-evaluation");
        logRepository.save(execLog);
        try {
            int fired = alertEvaluationService.evaluateAll();
            execLog.complete(fired, fired);
            if (fired > 0) {
                log.info("AlertEvaluationJob complete — {} alert(s) fired", fired);
            }
        } catch (Exception e) {
            execLog.fail(e.getMessage());
            log.error("AlertEvaluationJob failed", e);
        } finally {
            logRepository.save(execLog);
        }
    }

    private boolean withinTradingWindow() {
        ZonedDateTime now = ZonedDateTime.now(US_EASTERN);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime t = now.toLocalTime();
        return !t.isBefore(SESSION_OPEN) && !t.isAfter(SESSION_CLOSE);
    }
}
