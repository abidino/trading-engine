package com.trading.orchestration.domain;

import com.trading.orchestration.domain.model.AnalysisResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for async analysis run tracking.
 * Cleared on restart — fine for single-instance deployment.
 */
@Component
public class AnalysisRunStore {

    private final Map<String, RunState> runs = new ConcurrentHashMap<>();

    public String createRun(String ticker) {
        String runId = UUID.randomUUID().toString();
        runs.put(runId, new RunState(runId, ticker, "running", Instant.now(), null, null, null));
        return runId;
    }

    public void completeRun(String runId, AnalysisResult result) {
        runs.computeIfPresent(runId, (id, state) ->
                new RunState(id, state.ticker(), "completed", state.startedAt(), Instant.now(), result, null));
    }

    public void failRun(String runId, String error) {
        runs.computeIfPresent(runId, (id, state) ->
                new RunState(id, state.ticker(), "failed", state.startedAt(), Instant.now(), null, error));
    }

    public Optional<RunState> findById(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    public List<RunState> findAll() {
        return runs.values().stream()
                .sorted(Comparator.comparing(RunState::startedAt).reversed())
                .toList();
    }

    public List<RunState> findByTicker(String ticker) {
        return runs.values().stream()
                .filter(r -> r.ticker().equals(ticker))
                .sorted(Comparator.comparing(RunState::startedAt).reversed())
                .toList();
    }

    public record RunState(
            String runId,
            String ticker,
            String status,
            Instant startedAt,
            Instant completedAt,
            AnalysisResult result,
            String error
    ) {}
}
