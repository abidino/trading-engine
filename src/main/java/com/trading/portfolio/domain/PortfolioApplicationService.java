package com.trading.portfolio.domain;

import com.trading.marketdata.domain.port.in.IntradayQuoteUseCase;
import com.trading.marketdata.domain.port.in.MarketDataApplicationService;
import com.trading.portfolio.domain.model.PortfolioPosition;
import com.trading.portfolio.domain.model.PortfolioTransaction;
import com.trading.portfolio.domain.model.TransactionType;
import com.trading.portfolio.domain.port.in.PortfolioUseCase;
import com.trading.portfolio.domain.port.out.PortfolioPositionRepository;
import com.trading.portfolio.domain.port.out.PortfolioTransactionRepository;
import com.trading.portfolio.domain.service.PortfolioDomainService;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.event.AnalysisRequested;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Transactional
public class PortfolioApplicationService implements PortfolioUseCase {

    private final PortfolioDomainService domainService;
    private final PortfolioPositionRepository positionRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MarketDataApplicationService marketDataApplicationService;
    private final IntradayQuoteUseCase intradayQuoteUseCase;

    /** Per-ticker throttle so rapid page loads don't hammer the quote provider. */
    private final Map<String, Instant> lastQuoteRefresh = new ConcurrentHashMap<>();
    private static final Duration QUOTE_REFRESH_TTL = Duration.ofSeconds(30);

    public PortfolioApplicationService(
            PortfolioPositionRepository positionRepository,
            PortfolioTransactionRepository transactionRepository,
            ApplicationEventPublisher eventPublisher,
            MarketDataApplicationService marketDataApplicationService,
            IntradayQuoteUseCase intradayQuoteUseCase) {
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
        this.marketDataApplicationService = marketDataApplicationService;
        this.intradayQuoteUseCase = intradayQuoteUseCase;
        this.domainService = new PortfolioDomainService(positionRepository, transactionRepository);
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioPosition> listActivePositions() {
        List<PortfolioPosition> allActive = positionRepository.findAllActive();
        List<String> allLot = allActive.stream()
                .map(portfolioPosition -> portfolioPosition.getTicker().value())
                .toList();

        Map<String, BigDecimal> latestPriceForTickers = marketDataApplicationService.getLatestPriceForTickers(allLot);
        for (PortfolioPosition portfolioPosition : allActive) {
            BigDecimal latestPrice = latestPriceForTickers.get(portfolioPosition.getTicker().value());
            if (latestPrice != null && latestPrice.signum() > 0) {
                portfolioPosition.updateCurrentMarketPrice(new Money(latestPrice));
            }
        }
        return allActive;
    }

    /**
     * Best-effort refresh of live intraday quotes for every held ticker. Called on
     * page load so the user always sees the latest status. Throttled per ticker and
     * resilient to provider failures (e.g. rate limits) — never throws.
     */
    @Override
    public void refreshHeldQuotes() {
        Instant now = Instant.now();
        for (PortfolioPosition position : positionRepository.findAllActive()) {
            String ticker = position.getTicker().value();
            Instant last = lastQuoteRefresh.get(ticker);
            if (last != null && Duration.between(last, now).compareTo(QUOTE_REFRESH_TTL) < 0) {
                continue;
            }
            try {
                intradayQuoteUseCase.refreshQuote(ticker);
                lastQuoteRefresh.put(ticker, now);
            } catch (Exception e) {
                log.debug("Quote refresh skipped for {}: {}", ticker, e.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioPosition> listAllPositions() {
        return positionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioTransaction> listTransactions(Ticker ticker) {
        return transactionRepository.findByTicker(ticker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioTransaction> listAllTransactions() {
        return transactionRepository.findAll();
    }

    @Override
    public PortfolioTransaction recordTransaction(
            Ticker ticker, TransactionType type,
            BigDecimal quantity, Money price, Money commission) {
        PortfolioTransaction tx = domainService.recordTransaction(ticker, type, quantity, price, commission);
        log.info("Recorded {} {} qty={} @{}", type, ticker, quantity, price);
        return tx;
    }

    @Override
    public void updateStopLoss(UUID positionId, Money stopLoss) {
        positionRepository.findById(positionId).ifPresent(pos -> {
            pos.updateStopLoss(stopLoss);
            positionRepository.save(pos);
        });
    }

    @Override
    public void requestAnalysis(Ticker ticker) {
        Map<String, String> meta = positionRepository.findByTicker(ticker)
                .map(pos -> Map.of(
                        "entryPrice", pos.getAverageEntryPrice().amount().toPlainString(),
                        "quantity", pos.getQuantity().toPlainString()))
                .orElseGet(Map::of);
        eventPublisher.publishEvent(AnalysisRequested.of(ticker, AnalysisRequestType.PORTFOLIO_REVIEW, meta));
        log.info("AnalysisRequested published for portfolio position: {}", ticker);
    }
}
