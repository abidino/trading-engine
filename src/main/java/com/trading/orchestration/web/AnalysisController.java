package com.trading.orchestration.web;

import com.trading.orchestration.domain.AnalysisRunStore;
import com.trading.orchestration.domain.model.AnalysisRequest;
import com.trading.orchestration.domain.model.AnalysisResult;
import com.trading.orchestration.domain.port.in.RequestAnalysisUseCase;
import com.trading.portfolio.domain.port.in.PortfolioUseCase;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Ticker;
import com.trading.watchlist.domain.port.in.WatchlistUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final RequestAnalysisUseCase requestAnalysisUseCase;
    private final AnalysisRunStore runStore;
    private final PortfolioUseCase portfolioUseCase;
    private final WatchlistUseCase watchlistUseCase;

    /**
     * Async trigger: returns runId immediately, runs analysis in background.
     * Frontend polls /status/{runId} until status != "running".
     */
    @PostMapping("/run")
    public ResponseEntity<RunStartedResponse> startAnalysis(@Valid @RequestBody RunRequest dto) {
        String runId = runStore.createRun(dto.ticker());
        AnalysisRequestType type = dto.requestType() != null
                ? dto.requestType()
                : detectType(dto.ticker());
        CompletableFuture.runAsync(() -> {
            try {
                AnalysisRequest req = new AnalysisRequest(
                        new Ticker(dto.ticker()), type, Map.of());
                AnalysisResult result = requestAnalysisUseCase.analyze(req);
                runStore.completeRun(runId, result);
            } catch (Exception e) {
                log.error("Async analysis failed for {}: {}", dto.ticker(), e.getMessage());
                runStore.failRun(runId, e.getMessage());
            }
        });
        return ResponseEntity.ok(new RunStartedResponse(runId));
    }

    /**
     * Auto-detects the analysis intent from where the ticker currently lives:
     * an open portfolio position → SELL/HOLD review; a watchlist entry → BUY/WAIT review;
     * otherwise treat it as a fresh discovery candidate.
     */
    private AnalysisRequestType detectType(String ticker) {
        String symbol = ticker.toUpperCase();
        boolean inPortfolio = portfolioUseCase.listActivePositions().stream()
                .anyMatch(p -> p.getTicker().value().equalsIgnoreCase(symbol));
        if (inPortfolio) {
            return AnalysisRequestType.PORTFOLIO_REVIEW;
        }
        boolean inWatchlist = watchlistUseCase.listAll().stream()
                .anyMatch(w -> w.getTicker().value().equalsIgnoreCase(symbol));
        if (inWatchlist) {
            return AnalysisRequestType.WATCHLIST_REVIEW;
        }
        return AnalysisRequestType.DISCOVERY;
    }

    @GetMapping("/suggest-type/{ticker}")
    public ResponseEntity<Map<String, String>> suggestType(@PathVariable String ticker) {
        return ResponseEntity.ok(Map.of("requestType", detectType(ticker).name()));
    }

    @GetMapping("/status/{runId}")
    public ResponseEntity<AnalysisRunResponse> status(@PathVariable String runId) {
        return runStore.findById(runId)
                .map(s -> ResponseEntity.ok(AnalysisRunResponse.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/runs")
    public ResponseEntity<List<AnalysisRunResponse>> listRuns() {
        return ResponseEntity.ok(runStore.findAll().stream()
                .map(AnalysisRunResponse::from).toList());
    }

    @GetMapping("/history/{ticker}")
    public ResponseEntity<List<AnalysisRunResponse>> history(@PathVariable String ticker) {
        return ResponseEntity.ok(runStore.findByTicker(ticker).stream()
                .map(AnalysisRunResponse::from).toList());
    }

    /**
     * Synchronous direct analysis (original endpoint, kept for programmatic use).
     */
    @PostMapping
    public ResponseEntity<AnalysisResultResponse> analyze(@Valid @RequestBody AnalysisRequestDto dto) {
        AnalysisRequest request = new AnalysisRequest(
                new Ticker(dto.ticker()),
                dto.requestType(),
                dto.contextMetadata() != null ? dto.contextMetadata() : Map.of()
        );
        AnalysisResult result = requestAnalysisUseCase.analyze(request);
        return ResponseEntity.ok(AnalysisResultResponse.from(result));
    }
}
