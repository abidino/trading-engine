package com.trading.notification.domain;

import com.trading.decisionlog.domain.model.DecisionRecord;
import com.trading.decisionlog.domain.port.out.DecisionRecordRepository;
import com.trading.marketdata.domain.model.IntradayQuote;
import com.trading.marketdata.domain.port.in.IntradayQuoteUseCase;
import com.trading.notification.domain.model.AlertChannel;
import com.trading.notification.domain.model.AlertLog;
import com.trading.notification.domain.model.AlertType;
import com.trading.notification.domain.model.TriggeredAlert;
import com.trading.notification.domain.port.out.AlertLogRepository;
import com.trading.notification.domain.port.out.NotificationDeliveryPort;
import com.trading.notification.domain.port.out.TriggeredAlertRepository;
import com.trading.portfolio.domain.model.PortfolioPosition;
import com.trading.portfolio.domain.port.in.PortfolioUseCase;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.TradingLevels;
import com.trading.watchlist.domain.port.in.WatchlistUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Evaluates proactive threshold alerts for watched + held tickers using the actionable
 * levels already produced by the analysis pipeline. Purely informational — never trades.
 *
 * <p>For each ticker it compares the latest intraday price to the latest decision's
 * stop-loss / take-profit / entry levels, and separately checks whole-portfolio daily
 * drop. Each condition fires at most once per calendar day (de-duplicated via
 * {@link TriggeredAlertRepository}).</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AlertEvaluationService {

    private static final Set<String> ENTRY_ACTIONS = Set.of("BUY", "WAIT", "ADD_TO_WATCHLIST");

    private final PortfolioUseCase portfolioUseCase;
    private final WatchlistUseCase watchlistUseCase;
    private final DecisionRecordRepository decisionRepository;
    private final IntradayQuoteUseCase intradayQuotes;
    private final NotificationDeliveryPort emailDelivery;
    private final TriggeredAlertRepository triggeredAlerts;
    private final AlertLogRepository alertLogRepository;

    @Value("${alerts.portfolio-drop-pct:5.0}")
    private double portfolioDropPct;

    /**
     * Returns the freshest quote for the ticker: pulls a live refresh from the provider
     * (which captures pre/post-market prints when the regular session is closed) and falls
     * back to the last persisted quote when the refresh fails. This keeps stop-loss / entry
     * threshold checks anchored to the true latest price regardless of trading session.
     */
    private Optional<IntradayQuote> freshQuote(String ticker) {
        try {
            IntradayQuote refreshed = intradayQuotes.refreshQuote(ticker);
            if (refreshed != null && refreshed.price() != null) {
                return Optional.of(refreshed);
            }
        } catch (Exception e) {
            log.debug("Quote refresh failed for {} — using last persisted quote: {}", ticker, e.getMessage());
        }
        return intradayQuotes.latestQuote(ticker);
    }

    /** Runs the full evaluation over portfolio + watchlist; returns the number of alerts fired. */
    public int evaluateAll() {
        LocalDate today = LocalDate.now();
        int fired = 0;

        // Cost basis per held ticker → lets us gate stop-loss / take-profit alerts against the
        // user's actual average entry price instead of the generically-computed analysis levels.
        Map<String, Double> costBasisByTicker = new HashMap<>();
        for (PortfolioPosition p : portfolioUseCase.listActivePositions()) {
            if (p.getAverageEntryPrice() != null) {
                costBasisByTicker.put(p.getTicker().value(),
                        p.getAverageEntryPrice().amount().doubleValue());
            }
        }

        for (String ticker : collectTickers()) {
            Optional<IntradayQuote> quote = freshQuote(ticker);
            if (quote.isEmpty() || quote.get().price() == null) {
                continue;
            }
            double price = quote.get().price().doubleValue();
            if (price <= 0) {
                continue;
            }
            Optional<DecisionRecord> decision = decisionRepository.findLatestByTicker(new Ticker(ticker));
            if (decision.isEmpty()) {
                continue;
            }
            for (AlertCandidate c : evaluateTicker(ticker, price, decision.get().getAction(),
                    decision.get().getLevels(), costBasisByTicker.get(ticker))) {
                if (dispatch(ticker, c, today)) {
                    fired++;
                }
            }
        }

        AlertCandidate portfolioDrop = evaluatePortfolioDrop();
        if (portfolioDrop != null && dispatch("PORTFOLIO", portfolioDrop, today)) {
            fired++;
        }
        return fired;
    }

    /**
     * Pure rule evaluation for one ticker — no side effects, so it is unit-testable.
     * Returns the alert conditions currently met (before de-duplication).
     *
     * @param costBasis the holder's average entry price when this ticker is an <b>open position</b>,
     *                  or {@code null} for a watchlist-only candidate. When present, stop-loss and
     *                  take-profit alerts are gated so they only fire when the level is meaningful
     *                  relative to the user's cost: a stop above cost is not a loss, and a target
     *                  below cost is not a profit — neither should notify.
     */
    static List<AlertCandidate> evaluateTicker(String ticker, double price, String action,
                                               TradingLevels levels, Double costBasis) {
        List<AlertCandidate> out = new ArrayList<>();
        if (levels == null) {
            return out;
        }

        Double stop = levels.stopLoss();
        Double target = levels.takeProfit();

        // Sanity guard: for a long setup a valid stop must sit below the target. When the levels are
        // inverted or equal (stop >= target) they are unusable — firing on them produces contradictory
        // "stop-loss breached" + "take-profit reached" alerts for the same price, so skip both.
        boolean invertedLevels = stop != null && target != null && stop >= target;

        // STOP_LOSS: price broke at/below the protective stop. For a held position the stop must
        // represent a real loss (at/below cost basis); a stop above cost is a profit-locking exit,
        // not a loss, and must not alert as one.
        if (stop != null && price <= stop && !invertedLevels
                && (costBasis == null || stop <= costBasis)) {
            out.add(new AlertCandidate(AlertType.STOP_LOSS, stop,
                    "%s stop-loss seviyesini kırdı: fiyat %.2f ≤ stop %.2f"
                            .formatted(ticker, price, stop)));
        }

        // TAKE_PROFIT: price reached the target. For a held position the target must be a real gain
        // (at/above cost basis); a target below cost is a loss and must not alert as a profit.
        if (target != null && price >= target && !invertedLevels
                && (costBasis == null || target >= costBasis)) {
            out.add(new AlertCandidate(AlertType.TAKE_PROFIT, target,
                    "%s hedef fiyata ulaştı: fiyat %.2f ≥ hedef %.2f"
                            .formatted(ticker, price, target)));
        }

        Double entry = entryLevel(levels);
        boolean aboveStop = stop == null || price > stop;
        if (entry != null && action != null && ENTRY_ACTIONS.contains(action)
                && price <= entry && aboveStop) {
            out.add(new AlertCandidate(AlertType.ENTRY_ZONE, entry,
                    "%s giriş bölgesine geldi: fiyat %.2f ≤ giriş %.2f"
                            .formatted(ticker, price, entry)));
        }
        return out;
    }

    /** Preferred entry reference: ideal, then buy-zone high, then safe. */
    private static Double entryLevel(TradingLevels l) {
        if (l.idealEntry() != null) return l.idealEntry();
        if (l.entryHigh() != null) return l.entryHigh();
        if (l.safeEntry() != null) return l.safeEntry();
        return null;
    }

    /** Aggregates held positions vs their previous close; alerts when the daily drop exceeds threshold. */
    private AlertCandidate evaluatePortfolioDrop() {
        List<PortfolioPosition> positions = portfolioUseCase.listActivePositions();
        double todayValue = 0.0;
        double prevValue = 0.0;
        for (PortfolioPosition p : positions) {
            Optional<IntradayQuote> q = intradayQuotes.latestQuote(p.getTicker().value());
            if (q.isEmpty() || q.get().price() == null || q.get().previousClose() == null) {
                continue;
            }
            double qty = p.getQuantity().doubleValue();
            todayValue += qty * q.get().price().doubleValue();
            prevValue += qty * q.get().previousClose().doubleValue();
        }
        if (prevValue <= 0) {
            return null;
        }
        double changePct = (todayValue - prevValue) / prevValue * 100.0;
        if (changePct <= -portfolioDropPct) {
            return new AlertCandidate(AlertType.PORTFOLIO_DROP, portfolioDropPct,
                    "Portföy bugün %.1f%% düştü".formatted(changePct));
        }
        return null;
    }

    /** Persists + notifies a candidate if it hasn't already fired today. Returns true when sent. */
    private boolean dispatch(String ticker, AlertCandidate c, LocalDate day) {
        if (triggeredAlerts.existsForDay(ticker, c.type(), day)) {
            return false;
        }
        double price = c.type() == AlertType.PORTFOLIO_DROP ? 0.0 : c.level();
        triggeredAlerts.save(TriggeredAlert.of(ticker, c.type(), day, price, c.level(), c.message()));

        String subject = "[TradingEngine] %s uyarısı: %s".formatted(ticker, c.type());
        try {
            emailDelivery.deliverMessage(subject, c.message());
            alertLogRepository.save(AlertLog.sent(ticker, c.type().name(), AlertChannel.EMAIL));
            log.info("Threshold alert fired: {} {} — {}", ticker, c.type(), c.message());
        } catch (Exception e) {
            alertLogRepository.save(AlertLog.failed(ticker, c.type().name(), AlertChannel.EMAIL,
                    e.getMessage()));
            log.error("Threshold alert delivery failed for {} {}", ticker, c.type(), e);
        }
        return true;
    }

    /** Distinct, order-preserving union of portfolio + watchlist tickers. */
    private Set<String> collectTickers() {
        Set<String> tickers = new LinkedHashSet<>();
        portfolioUseCase.listActivePositions().forEach(p -> tickers.add(p.getTicker().value()));
        watchlistUseCase.listAll().forEach(w -> tickers.add(w.getTicker().value()));
        return tickers;
    }

    @Transactional(readOnly = true)
    public List<TriggeredAlert> listRecent(int limit) {
        return triggeredAlerts.findRecent(limit);
    }

    /** Immutable alert condition produced by rule evaluation. */
    record AlertCandidate(AlertType type, double level, String message) {}
}
