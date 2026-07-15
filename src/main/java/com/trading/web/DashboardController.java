package com.trading.web;

import com.trading.decisionlog.domain.DecisionLogApplicationService;
import com.trading.decisionlog.domain.model.DecisionOutcome;
import com.trading.decisionlog.domain.model.DecisionRecord;
import com.trading.portfolio.domain.model.PortfolioPosition;
import com.trading.portfolio.domain.port.in.PortfolioUseCase;
import com.trading.watchlist.domain.port.in.WatchlistUseCase;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Ticker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.trading.shared.kernel.event.AnalysisRequested;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cross-domain dashboard aggregation controller.
 * Lives in app module — the only module that can see all domain services.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PortfolioUseCase portfolioUseCase;
    private final WatchlistUseCase watchlistUseCase;
    private final DecisionLogApplicationService decisionLogService;
    private final ApplicationEventPublisher eventPublisher;

    /** Full portfolio snapshot for the dashboard header cards. */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> summary() {
        List<PortfolioPosition> positions = portfolioUseCase.listActivePositions();
        List<DecisionRecord> decisions = decisionLogService.listAll();

        double totalMarketValue = positions.stream().mapToDouble(p -> p.marketValue().doubleValue()).sum();
        double totalCostBasis = positions.stream().mapToDouble(p -> p.costBasis().doubleValue()).sum();
        double totalUnrealizedPnl = totalMarketValue - totalCostBasis;
        double totalUnrealizedPnlPercent = totalCostBasis != 0
                ? totalUnrealizedPnl / totalCostBasis * 100.0 : 0.0;

        List<TickerPnl> ranked = positions.stream()
                .map(p -> new TickerPnl(
                        p.getTicker().value(),
                        p.unrealizedPnl().doubleValue(),
                        p.unrealizedPnlPercent().doubleValue()))
                .sorted(Comparator.comparingDouble(TickerPnl::pnl).reversed())
                .toList();
        List<TickerPnl> topGainers = ranked.stream().filter(t -> t.pnl() > 0).limit(5).toList();
        List<TickerPnl> topLosers = ranked.stream().filter(t -> t.pnl() < 0)
                .sorted(Comparator.comparingDouble(TickerPnl::pnl))
                .limit(5).toList();

        long validated = decisions.stream()
                .filter(d -> d.getOutcome() == DecisionOutcome.VALIDATED).count();
        long evaluated = decisions.stream()
                .filter(d -> d.getOutcome() != DecisionOutcome.PENDING).count();
        double accuracy = evaluated > 0
                ? (double) validated / evaluated * 100.0 : 0.0;

        return ResponseEntity.ok(new DashboardSummary(
                totalMarketValue, totalCostBasis,
                totalUnrealizedPnl, Math.round(totalUnrealizedPnlPercent * 100.0) / 100.0,
                positions.size(), Map.of(),
                topGainers, topLosers,
                watchlistUseCase.listAll().size(),
                Math.round(accuracy * 10.0) / 10.0
        ));
    }

    /** Sector breakdown — grouped by ticker (sector not stored, returns one "Mixed" bucket). */
    @GetMapping("/sectors")
    public ResponseEntity<List<SectorBreakdown>> sectors() {
        List<PortfolioPosition> positions = portfolioUseCase.listActivePositions();
        if (positions.isEmpty()) return ResponseEntity.ok(List.of());

        double totalValue = positions.stream().mapToDouble(p -> p.marketValue().doubleValue()).sum();

        List<String> tickers = positions.stream()
                .map(p -> p.getTicker().value()).toList();

        return ResponseEntity.ok(List.of(
                new SectorBreakdown("Mixed", totalValue, 100.0, tickers)
        ));
    }

    /** Per-position performance summary. */
    @GetMapping("/performance")
    public ResponseEntity<List<PerformanceEntry>> performance() {
        List<PortfolioPosition> positions = portfolioUseCase.listActivePositions();
        List<DecisionRecord> decisions = decisionLogService.listAll();

        Map<String, Long> decisionCountByTicker = decisions.stream()
                .collect(Collectors.groupingBy(d -> d.getTicker().value(), Collectors.counting()));

        Map<String, Long> winsByTicker = decisions.stream()
                .filter(d -> d.getOutcome() == DecisionOutcome.VALIDATED)
                .collect(Collectors.groupingBy(d -> d.getTicker().value(), Collectors.counting()));

        List<PerformanceEntry> entries = positions.stream().map(p -> {
            String ticker = p.getTicker().value();
            double marketValue = p.marketValue().doubleValue();
            double costBasis = p.costBasis().doubleValue();
            double pnl = p.unrealizedPnl().doubleValue();
            double pnlPct = p.unrealizedPnlPercent().doubleValue();
            long dCount = decisionCountByTicker.getOrDefault(ticker, 0L);
            long wins = winsByTicker.getOrDefault(ticker, 0L);
            double winRate = dCount > 0 ? (double) wins / dCount * 100.0 : 0.0;
            return new PerformanceEntry(ticker, marketValue, costBasis,
                    pnl, Math.round(pnlPct * 100.0) / 100.0,
                    (int) dCount, Math.round(winRate * 10.0) / 10.0);
        }).toList();

        return ResponseEntity.ok(entries);
    }

    /** Triggers analysis for every active portfolio position. */
    @PostMapping("/run-analysis")
    public ResponseEntity<Void> runAnalysis() {
        portfolioUseCase.listActivePositions().forEach(p ->
                eventPublisher.publishEvent(
                        AnalysisRequested.of(p.getTicker(), AnalysisRequestType.PORTFOLIO_REVIEW)));
        return ResponseEntity.accepted().build();
    }

    /** Approves all pending watchlist items (promotes from discovery drafts). */
    @PostMapping("/auto-promote")
    public ResponseEntity<Void> autoPromote() {
        watchlistUseCase.listAll().stream()
                .filter(item -> !item.isApproved())
                .forEach(item -> watchlistUseCase.approvePendingItem(item.getId()));
        return ResponseEntity.ok().build();
    }
}
