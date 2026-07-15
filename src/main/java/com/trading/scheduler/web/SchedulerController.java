package com.trading.scheduler.web;

import com.trading.scheduler.domain.job.DiscoveryJob;
import com.trading.scheduler.domain.job.IntradayQuoteJob;
import com.trading.scheduler.domain.job.NewsScanJob;
import com.trading.scheduler.domain.job.PortfolioAnalysisJob;
import com.trading.scheduler.domain.job.TechnicalTrendJob;
import com.trading.scheduler.domain.job.WatchlistAnalysisJob;
import com.trading.scheduler.domain.port.out.JobExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SchedulerController {

    private final JobExecutionLogRepository logRepository;
    private final DiscoveryJob discoveryJob;
    private final PortfolioAnalysisJob portfolioAnalysisJob;
    private final WatchlistAnalysisJob watchlistAnalysisJob;
    private final TechnicalTrendJob technicalTrendJob;
    private final NewsScanJob newsScanJob;
    private final IntradayQuoteJob intradayQuoteJob;

    @Value("${scheduler.discovery.cron:0 0 5 * * MON-FRI}")
    private String discoveryCron;
    @Value("${scheduler.portfolio.cron:0 0 0,5,11,15,19,23 * * *}")
    private String portfolioCron;
    @Value("${scheduler.watchlist.cron:0 0 0,5,11,15,19,23 * * *}")
    private String watchlistCron;
    @Value("${scheduler.technical.cron:0 30 21 * * MON-FRI}")
    private String technicalCron;
    @Value("${scheduler.news.cron:0 0 0,4,5,10,11,12,13,14,15,16,17,18,19,20,21,22,23 * * *}")
    private String newsCron;
    @Value("${scheduler.intraday.cron:0 0/10 * * * *}")
    private String intradayCron;

    /** Scheduler meta-status: list of registered cron jobs. */
    @GetMapping("/api/v1/scheduler/status")
    public ResponseEntity<SchedulerStatus> schedulerStatus() {
        return ResponseEntity.ok(new SchedulerStatus(true, List.of(
                new JobInfo("discovery", "cron", discoveryCron),
                new JobInfo("portfolio-analysis", "cron", portfolioCron),
                new JobInfo("watchlist-analysis", "cron", watchlistCron),
                new JobInfo("technical-trend", "cron", technicalCron),
                new JobInfo("news-scan", "cron", newsCron),
                new JobInfo("intraday-quote", "cron", intradayCron)
        )));
    }

    /** Most recent job executions (across all jobs). */
    @GetMapping("/api/v1/jobs/recent")
    public ResponseEntity<List<JobLogResponse>> listRecent(
            @RequestParam(defaultValue = "50") int limit) {
        List<JobLogResponse> results = logRepository.findAll().stream()
                .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                .limit(limit)
                .map(JobLogResponse::from)
                .toList();
        return ResponseEntity.ok(results);
    }

    /** History for a specific job. */
    @GetMapping("/api/v1/jobs/{jobName}")
    public ResponseEntity<List<JobLogResponse>> listByJob(
            @PathVariable String jobName,
            @RequestParam(defaultValue = "20") int limit) {
        List<JobLogResponse> results = logRepository.findByJobName(jobName).stream()
                .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                .limit(limit)
                .map(JobLogResponse::from)
                .toList();
        return ResponseEntity.ok(results);
    }

    /** Manually trigger a scheduled job by name. */
    @PostMapping("/api/v1/jobs/{jobName}/trigger")
    public ResponseEntity<Void> trigger(@PathVariable String jobName) {
        switch (jobName) {
            case "discovery" -> discoveryJob.run();
            case "portfolio-analysis" -> portfolioAnalysisJob.run();
            case "watchlist-analysis" -> watchlistAnalysisJob.run();
            case "technical-trend" -> technicalTrendJob.run();
            case "news-scan" -> newsScanJob.run();
            case "intraday-quote" -> intradayQuoteJob.run();
            default -> { return ResponseEntity.notFound().build(); }
        }
        return ResponseEntity.accepted().build();
    }
}
