package com.trading.decisionlog.domain.model;

import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.TradingLevels;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate Root: the immutable audit record of a single AI decision.
 * Never deleted. Only the outcome and evaluatedAt fields may be filled in later.
 */
public class DecisionRecord {

    private final UUID id;
    private final Ticker ticker;
    private final AnalysisRequestType decisionType;
    private final String action;
    private final double confidence;
    private final String reasoning;
    private final String technicalSummary;
    private final String fundamentalSummary;
    private final String newsSummary;
    private final String socialSummary;
    private final String counterThesis;
    private final List<String> keyRisks;
    private final TradingLevels levels;
    private final Instant decidedAt;
    private DecisionOutcome outcome;
    private Instant evaluatedAt;

    public DecisionRecord(UUID id, Ticker ticker, AnalysisRequestType decisionType,
                          String action, double confidence, String reasoning,
                          String technicalSummary, String fundamentalSummary,
                          String newsSummary, String socialSummary, Instant decidedAt) {
        this(id, ticker, decisionType, action, confidence, reasoning,
                technicalSummary, fundamentalSummary, newsSummary, socialSummary,
                "", List.of(), TradingLevels.empty(), decidedAt);
    }

    public DecisionRecord(UUID id, Ticker ticker, AnalysisRequestType decisionType,
                          String action, double confidence, String reasoning,
                          String technicalSummary, String fundamentalSummary,
                          String newsSummary, String socialSummary,
                          String counterThesis, List<String> keyRisks,
                          TradingLevels levels, Instant decidedAt) {
        this.id = id;
        this.ticker = ticker;
        this.decisionType = decisionType;
        this.action = action;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.technicalSummary = technicalSummary;
        this.fundamentalSummary = fundamentalSummary;
        this.newsSummary = newsSummary;
        this.socialSummary = socialSummary;
        this.counterThesis = counterThesis != null ? counterThesis : "";
        this.keyRisks = keyRisks != null ? List.copyOf(keyRisks) : List.of();
        this.levels = levels != null ? levels : TradingLevels.empty();
        this.decidedAt = decidedAt;
        this.outcome = DecisionOutcome.PENDING;
    }

    public void recordOutcome(DecisionOutcome outcome) {
        if (this.outcome != DecisionOutcome.PENDING) {
            throw new IllegalStateException("Outcome already recorded for decision " + id);
        }
        this.outcome = outcome;
        this.evaluatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Ticker getTicker() { return ticker; }
    public AnalysisRequestType getDecisionType() { return decisionType; }
    public String getAction() { return action; }
    public double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public String getTechnicalSummary() { return technicalSummary; }
    public String getFundamentalSummary() { return fundamentalSummary; }
    public String getNewsSummary() { return newsSummary; }
    public String getSocialSummary() { return socialSummary; }
    public String getCounterThesis() { return counterThesis; }
    public List<String> getKeyRisks() { return keyRisks; }
    public TradingLevels getLevels() { return levels; }
    public Instant getDecidedAt() { return decidedAt; }
    public DecisionOutcome getOutcome() { return outcome; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
