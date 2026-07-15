package com.trading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * Concurrency configuration.
 *
 * <p>Two problems this fixes:</p>
 * <ul>
 *   <li>The default Spring {@code @Scheduled} scheduler is <b>single-threaded</b>, so a
 *       slow job (an LLM analysis sweep) blocks every other job (intraday quotes, news).
 *       {@link #taskScheduler()} gives {@code @Scheduled} a small pool instead.</li>
 *   <li>Analysis is triggered via {@code AnalysisRequested} events. Handled synchronously,
 *       each LLM call blocks the publisher (a scheduler thread or the request thread) and
 *       tickers are analysed one-at-a-time inline. The {@link #analysisExecutor()} pool lets
 *       the async event listener run analyses off the publisher's thread.</li>
 * </ul>
 *
 * <p>The analysis pool is intentionally small because local Ollama inference is heavy —
 * too much concurrency would overload the model server and slow every request down.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** Multi-threaded scheduler for {@code @Scheduled} jobs (replaces the single-thread default). */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sched-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.setAwaitTerminationSeconds(20);
        scheduler.initialize();
        return scheduler;
    }

    /** Dedicated pool for asynchronous LLM analysis so it never blocks the scheduler. */
    @Bean(name = "analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("analysis-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
