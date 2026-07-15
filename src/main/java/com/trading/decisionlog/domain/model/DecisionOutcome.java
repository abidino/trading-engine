package com.trading.decisionlog.domain.model;

/** Outcome of a past AI decision once the market has spoken. */
public enum DecisionOutcome {
    /** Decision was made; outcome not yet determined. */
    PENDING,
    /** Market action confirmed the AI's recommendation. */
    VALIDATED,
    /** Market action contradicted the AI's recommendation. */
    INVALIDATED
}
