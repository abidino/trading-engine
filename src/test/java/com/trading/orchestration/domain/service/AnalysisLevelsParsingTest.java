package com.trading.orchestration.domain.service;

import com.trading.shared.kernel.TradingLevels;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for trading-level parsing.
 *
 * <p>Guards against the "levels silently dropped" class of bug: whatever numeric levels the
 * LLM returns MUST end up on the parsed {@link TradingLevels}, whether the model nested them
 * under {@code levels} or emitted them flat, and blank/missing/non-numeric values must degrade
 * to {@code null} rather than crash the whole analysis.</p>
 */
class AnalysisLevelsParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode parse(String json) {
        return mapper.readTree(json);
    }

    @Test
    void parsesNestedLevelsObject() {
        TradingLevels l = AnalysisOrchestrationDomainService.parseLevels(parse("""
                {"action":"BUY","levels":{"entryLow":47.5,"entryHigh":49.0,
                 "stopLoss":44.0,"takeProfit":60.0,"nearestSupport":46.0,"nearestResistance":58.0}}
                """));
        assertEquals(47.5, l.entryLow());
        assertEquals(49.0, l.entryHigh());
        assertEquals(44.0, l.stopLoss());
        assertEquals(60.0, l.takeProfit());
        assertEquals(46.0, l.nearestSupport());
        assertEquals(58.0, l.nearestResistance());
        assertTrue(l.hasAnyLevel());
    }

    @Test
    void parsesFlatLevelsLayout() {
        TradingLevels l = AnalysisOrchestrationDomainService.parseLevels(parse("""
                {"action":"BUY","stopLoss":44.0,"takeProfit":60.0}
                """));
        assertEquals(44.0, l.stopLoss());
        assertEquals(60.0, l.takeProfit());
        assertNull(l.entryLow());
    }

    @Test
    void missingLevelsYieldNullsNotCrash() {
        TradingLevels l = AnalysisOrchestrationDomainService.parseLevels(parse("""
                {"action":"HOLD","confidence":0.6,"reasoning":"..."}
                """));
        assertNull(l.stopLoss());
        assertNull(l.takeProfit());
        assertFalse(l.hasAnyLevel());
    }

    @Test
    void blankOrNullOrStringNumbersAreHandled() {
        TradingLevels l = AnalysisOrchestrationDomainService.parseLevels(parse("""
                {"levels":{"entryLow":"","entryHigh":null,"stopLoss":"44.5","takeProfit":"abc"}}
                """));
        assertNull(l.entryLow());     // blank string
        assertNull(l.entryHigh());    // explicit null
        assertEquals(44.5, l.stopLoss()); // numeric string parsed
        assertNull(l.takeProfit());   // non-numeric string
    }

    @Test
    void parsesThreeStaggeredEntryPoints() {
        TradingLevels l = AnalysisOrchestrationDomainService.parseLevels(parse("""
                {"action":"BUY","levels":{"aggressiveEntry":49.0,"idealEntry":47.0,
                 "safeEntry":45.0,"stopLoss":44.0,"takeProfit":60.0}}
                """));
        assertEquals(49.0, l.aggressiveEntry());
        assertEquals(47.0, l.idealEntry());
        assertEquals(45.0, l.safeEntry());
        assertTrue(l.hasAnyLevel());
    }

    @Test
    void entryPointsOnlyStillCountAsLevels() {
        TradingLevels l = AnalysisOrchestrationDomainService.parseLevels(parse("""
                {"action":"BUY","idealEntry":100.5}
                """));
        assertEquals(100.5, l.idealEntry());
        assertNull(l.aggressiveEntry());
        assertTrue(l.hasAnyLevel());
    }

    // ── Counter-thesis & key risks (mandatory opposing view) ──

    @Test
    void parsesKeyRisksArray() {
        java.util.List<String> risks = AnalysisOrchestrationDomainService.parseStringList(
                parse("""
                {"keyRisks":["China export ban","valuation stretched","","  margin compression  "]}
                """).path("keyRisks"));
        assertEquals(3, risks.size());
        assertTrue(risks.contains("China export ban"));
        assertTrue(risks.contains("margin compression")); // trimmed, blank dropped
    }

    @Test
    void keyRisksAcceptsSingleStringOrMissing() {
        assertEquals(1, AnalysisOrchestrationDomainService.parseStringList(
                parse("{\"keyRisks\":\"single risk\"}").path("keyRisks")).size());
        assertTrue(AnalysisOrchestrationDomainService.parseStringList(
                parse("{\"action\":\"BUY\"}").path("keyRisks")).isEmpty());
    }

    // ── JSON sanitisation (local models wrap JSON in markdown fences / prose) ──

    @Test
    void stripsJsonMarkdownFence() {
        String raw = "```json\n{\"action\":\"BUY\",\"confidence\":0.8}\n```";
        String clean = AnalysisOrchestrationDomainService.sanitizeJson(raw);
        assertEquals("{\"action\":\"BUY\",\"confidence\":0.8}", clean);
    }

    @Test
    void stripsPlainTripleBacktickFence() {
        String raw = "```\n{\"action\":\"HOLD\"}\n```";
        assertEquals("{\"action\":\"HOLD\"}", AnalysisOrchestrationDomainService.sanitizeJson(raw));
    }

    @Test
    void narrowsToObjectWhenSurroundedByProse() {
        String raw = "Here is my analysis:\n{\"action\":\"SELL\",\"confidence\":0.6}\nHope that helps!";
        assertEquals("{\"action\":\"SELL\",\"confidence\":0.6}",
                AnalysisOrchestrationDomainService.sanitizeJson(raw));
    }

    @Test
    void passesThroughPlainJson() {
        String raw = "{\"action\":\"BUY\"}";
        assertEquals(raw, AnalysisOrchestrationDomainService.sanitizeJson(raw));
    }
}
