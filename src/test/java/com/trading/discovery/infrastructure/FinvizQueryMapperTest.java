package com.trading.discovery.infrastructure;

import com.trading.discovery.domain.model.DiscoveryFilter;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression coverage for the faithful-mirror Finviz query mapping.
 *
 * <p>The mapper is now a trivial {@code prefix + token} join driven by the scraped
 * {@code finviz/filter-catalog.json}. These tests guard two things:
 * (1) a selected filter is emitted with the EXACT Finviz token Finviz itself uses, and
 * (2) invalid/unknown selections are dropped (never silently sent as fake filters).</p>
 */
class FinvizQueryMapperTest {

    private final FinvizFilterCatalog catalog = new FinvizFilterCatalog(new ObjectMapper());
    private final FinvizQueryMapper mapper = new FinvizQueryMapper(catalog);

    private DiscoveryFilter of(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return new DiscoveryFilter(m, null);
    }

    private String tokens(DiscoveryFilter f) {
        return mapper.toFilterParam(f);
    }

    // ── Each filter family emits its exact Finviz token ──────────────────────

    @Test
    void valuationTokens() {
        assertEquals("fa_pe_u20", tokens(of("pe", "u20")));
        assertEquals("fa_fpe_o10", tokens(of("forwardPe", "o10")));
        assertEquals("fa_peg_u2", tokens(of("peg", "u2")));
        assertEquals("fa_ps_u3", tokens(of("priceSales", "u3")));
        assertEquals("fa_pfcf_u20", tokens(of("priceFcf", "u20")));
        assertEquals("fa_evebitda_u15", tokens(of("evEbitda", "u15")));
    }

    @Test
    void universeTokens() {
        assertEquals("geo_usa", tokens(of("country", "usa")));
        assertEquals("sec_technology", tokens(of("sector", "technology")));
        assertEquals("ind_semiconductors", tokens(of("industry", "semiconductors")));
        assertEquals("cap_largeunder", tokens(of("marketCap", "largeunder")));
    }

    @Test
    void momentumAndOwnershipTokens() {
        // 52-week high: faithful Finviz semantics — "within 10% of high" is b0to10h
        assertEquals("ta_highlow52w_b0to10h", tokens(of("high52w", "b0to10h")));
        assertEquals("sh_relvol_o1.5", tokens(of("relVol", "o1.5")));
        assertEquals("ta_rsi_os30", tokens(of("rsi", "os30")));
        assertEquals("sh_insiderown_o10", tokens(of("insiderOwn", "o10")));
        assertEquals("sh_instown_u50", tokens(of("instOwn", "u50")));
        assertEquals("targetprice_a20", tokens(of("targetPrice", "a20")));
    }

    @Test
    void multipleSelectionsJoinWithComma() {
        String t = tokens(of("sector", "technology", "country", "usa", "pe", "u20"));
        assertTrue(t.contains("sec_technology"));
        assertTrue(t.contains("geo_usa"));
        assertTrue(t.contains("fa_pe_u20"));
        assertEquals(2, t.chars().filter(c -> c == ',').count(), "3 tokens → 2 commas: " + t);
    }

    // ── Invalid selections are dropped, never sent as fake filters ───────────

    @Test
    void unknownFilterKeyIsDropped() {
        assertEquals("", tokens(of("madeUpFilter", "u20")));
    }

    @Test
    void invalidTokenForKnownKeyIsDropped() {
        // 'zzz99' is NOT a real Finviz RSI option → dropped
        assertEquals("", tokens(of("rsi", "zzz99")));
        // insider '5' has no o5 bucket on Finviz (min is o10) → dropped
        assertEquals("", tokens(of("insiderOwn", "5")));
    }

    @Test
    void rawPassthroughAppended() {
        String t = tokens(new DiscoveryFilter(Map.of("sector", "technology"), "sh_avgvol_o500"));
        assertTrue(t.contains("sec_technology"));
        assertTrue(t.contains("sh_avgvol_o500"));
    }

    @Test
    void emptyFilterYieldsEmptyString() {
        assertEquals("", tokens(of()));
    }

    @Test
    void defaultsUseOnlyValidTokens() {
        // Every token emitted by defaults() must be a real Finviz option (non-empty result,
        // and no dropped-key warnings would leave gaps). Just assert a few known ones present.
        String t = tokens(DiscoveryFilter.defaults());
        assertTrue(t.contains("geo_usa"), t);
        assertTrue(t.contains("cap_smallover"), t);
        assertTrue(t.contains("fa_peg_u2"), t);
        assertTrue(t.contains("targetprice_a5"), t);
    }
}
