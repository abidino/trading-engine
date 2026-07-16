package com.trading.portfolio.web;

import com.trading.portfolio.domain.model.PortfolioTransaction;
import com.trading.portfolio.domain.model.TransactionType;
import com.trading.portfolio.domain.port.in.PortfolioUseCase;
import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioUseCase portfolioUseCase;

    @GetMapping("/positions")
    public ResponseEntity<List<PositionResponse>> listPositions() {
        return ResponseEntity.ok(portfolioUseCase.listActivePositions()
                .stream().map(PositionResponse::from).toList());
    }

    /** Refreshes live quotes for held tickers, then returns the up-to-date positions. */
    @PostMapping("/refresh")
    public ResponseEntity<List<PositionResponse>> refreshAndListPositions() {
        portfolioUseCase.refreshHeldQuotes();
        return ResponseEntity.ok(portfolioUseCase.listActivePositions()
                .stream().map(PositionResponse::from).toList());
    }

    /** Refreshes live quote for a specific ticker. */
    @PostMapping("/refresh/{ticker}")
    public ResponseEntity<PositionResponse> refreshTickerQuote(@PathVariable String ticker) {
        portfolioUseCase.refreshTickerQuote(new Ticker(ticker));
        return portfolioUseCase.listActivePositions()
                .stream()
                .filter(p -> p.getTicker().value().equals(ticker))
                .findFirst()
                .map(PositionResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/positions/closed")
    public ResponseEntity<List<SoldPositionResponse>> listClosedPositions() {
        List<PortfolioTransaction> all = portfolioUseCase.listAllTransactions();
        Map<String, List<PortfolioTransaction>> byTicker = all.stream()
                .collect(Collectors.groupingBy(t -> t.ticker().value()));

        List<SoldPositionResponse> result = new ArrayList<>();
        byTicker.forEach((ticker, txns) -> {
            List<PortfolioTransaction> buys = txns.stream().filter(t -> t.transactionType() == TransactionType.BUY).toList();
            List<PortfolioTransaction> sells = txns.stream().filter(t -> t.transactionType() == TransactionType.SELL).toList();
            if (sells.isEmpty()) return;

            double totalSellQty = sells.stream().mapToDouble(t -> t.quantity().doubleValue()).sum();
            double totalBuyQty = buys.stream().mapToDouble(t -> t.quantity().doubleValue()).sum();
            double avgBuyPrice = totalBuyQty == 0 ? 0 :
                    buys.stream().mapToDouble(t -> t.price().amount().doubleValue() * t.quantity().doubleValue()).sum() / totalBuyQty;
            double avgSellPrice = sells.stream().mapToDouble(t -> t.price().amount().doubleValue() * t.quantity().doubleValue()).sum() / totalSellQty;
            double buyComm = buys.stream().mapToDouble(t -> t.commission().amount().doubleValue()).sum();
            double sellComm = sells.stream().mapToDouble(t -> t.commission().amount().doubleValue()).sum();
            double realizedPnl = (avgSellPrice - avgBuyPrice) * totalSellQty - buyComm - sellComm;
            String soldAt = sells.stream().map(t -> t.executedAt().toString()).max(Comparator.naturalOrder()).orElse(null);

            result.add(new SoldPositionResponse(ticker, totalSellQty, avgBuyPrice, avgSellPrice,
                    buyComm, sellComm, buyComm + sellComm, realizedPnl, soldAt));
        });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> listTransactions() {
        return ResponseEntity.ok(portfolioUseCase.listAllTransactions()
                .stream().map(TransactionResponse::from).toList());
    }

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryResponse> summary() {
        var active = portfolioUseCase.listActivePositions();
        var allTxns = portfolioUseCase.listAllTransactions();

        double totalMarketValue = 0.0;
        double totalGains = 0.0;
        double totalLosses = 0.0;
        int gainCount = 0;
        int lossCount = 0;
        for (var p : active) {
            double mv = p.marketValue().doubleValue();
            double pnl = p.unrealizedPnl().doubleValue();
            totalMarketValue += mv;
            if (pnl > 0) {
                totalGains += pnl;
                gainCount++;
            } else if (pnl < 0) {
                totalLosses += pnl;
                lossCount++;
            }
        }
        double totalPnl = totalGains + totalLosses;
        double totalCommissions = allTxns.stream().mapToDouble(t -> t.commission().amount().doubleValue()).sum();

        // Net P&L folds the entry-fee drag of still-open positions into unrealized P&L:
        // sum the BUY commissions of tickers we currently hold and subtract them.
        Set<String> activeTickers = active.stream()
                .map(p -> p.getTicker().value()).collect(Collectors.toSet());
        double openBuyCommissions = allTxns.stream()
                .filter(t -> t.transactionType() == TransactionType.BUY)
                .filter(t -> activeTickers.contains(t.ticker().value()))
                .mapToDouble(t -> t.commission().amount().doubleValue())
                .sum();
        double netPnl = totalPnl - openBuyCommissions;

        return ResponseEntity.ok(new PortfolioSummaryResponse(
                totalMarketValue, totalGains, totalLosses, totalPnl,
                netPnl, totalCommissions, active.size(), gainCount, lossCount));
    }

    @PostMapping("/transactions")
    public ResponseEntity<Void> recordTransaction(@Valid @RequestBody TransactionRequest dto) {
        portfolioUseCase.recordTransaction(
                new Ticker(dto.ticker()), dto.transactionType(), dto.quantity(),
                Money.of(dto.price()),
                dto.commission() != null ? Money.of(dto.commission()) : Money.zero());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/positions/{positionId}/stop-loss")
    public ResponseEntity<Void> updateStopLoss(@PathVariable UUID positionId, @RequestBody StopLossRequest dto) {
        portfolioUseCase.updateStopLoss(positionId, Money.of(dto.stopLoss()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/positions/{ticker}/analyze")
    public ResponseEntity<Void> requestAnalysis(@PathVariable String ticker) {
        portfolioUseCase.requestAnalysis(new Ticker(ticker));
        return ResponseEntity.accepted().build();
    }
}
