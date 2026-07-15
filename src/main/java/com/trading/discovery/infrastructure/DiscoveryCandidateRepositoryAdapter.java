package com.trading.discovery.infrastructure;

import com.trading.discovery.domain.model.DiscoveryCandidate;
import com.trading.discovery.domain.model.DiscoveryStatus;
import com.trading.discovery.domain.port.out.DiscoveryCandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscoveryCandidateRepositoryAdapter implements DiscoveryCandidateRepository {

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final JpaDiscoveryCandidateRepository jpa;
    private final ObjectMapper objectMapper;

    @Override
    public DiscoveryCandidate save(DiscoveryCandidate candidate) {
        // Upsert by ticker: reuse the existing row id when present.
        var entity = jpa.findByTicker(candidate.ticker())
                .orElseGet(DiscoveryCandidateEntity::new);
        applyDomain(entity, candidate);
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<DiscoveryCandidate> findByTicker(String ticker) {
        return jpa.findByTicker(ticker).map(this::toDomain);
    }

    @Override
    public List<DiscoveryCandidate> findByStatus(DiscoveryStatus status) {
        return jpa.findByStatusOrderByEvaluatedAtDesc(status).stream().map(this::toDomain).toList();
    }

    @Override
    public List<DiscoveryCandidate> findRecommended() {
        return findByStatus(DiscoveryStatus.RECOMMENDED);
    }

    // -----------------------------------------------------------------------

    private void applyDomain(DiscoveryCandidateEntity e, DiscoveryCandidate c) {
        e.setId(c.id());
        e.setTicker(c.ticker());
        e.setCompanyName(c.companyName());
        e.setSector(c.sector());
        e.setScreenerSource(c.screenerSource());
        e.setCriteriaJson(writeCriteria(c.matchedCriteria()));
        e.setStatus(c.status());
        e.setRecommended(c.recommended());
        e.setConfidence(c.confidence());
        e.setReasoning(c.reasoning());
        e.setTrendDirection(c.trendDirection());
        e.setDiscoveredAt(c.discoveredAt());
        e.setEvaluatedAt(c.evaluatedAt());
    }

    private DiscoveryCandidate toDomain(DiscoveryCandidateEntity e) {
        return new DiscoveryCandidate(
                e.getId(), e.getTicker(), e.getCompanyName(), e.getSector(),
                e.getScreenerSource(), readCriteria(e.getCriteriaJson()),
                e.getStatus(), e.isRecommended(), e.getConfidence(), e.getReasoning(),
                e.getTrendDirection(), e.getDiscoveredAt(), e.getEvaluatedAt());
    }

    private String writeCriteria(Map<String, String> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(criteria);
        } catch (Exception e) {
            log.warn("Could not serialize matchedCriteria: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> readCriteria(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            log.warn("Could not deserialize matchedCriteria: {}", e.getMessage());
            return Map.of();
        }
    }
}
