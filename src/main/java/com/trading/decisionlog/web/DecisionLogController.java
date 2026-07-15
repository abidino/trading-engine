package com.trading.decisionlog.web;

import com.trading.decisionlog.domain.DecisionLogApplicationService;
import com.trading.decisionlog.domain.model.AccuracyReport;
import com.trading.decisionlog.domain.model.DecisionOutcome;
import com.trading.shared.kernel.Ticker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionLogController {

    private final DecisionLogApplicationService service;

    @GetMapping
    public ResponseEntity<List<DecisionRecordResponse>> listAll() {
        return ResponseEntity.ok(service.listAll().stream().map(DecisionRecordResponse::from).toList());
    }

    @GetMapping("/ticker/{ticker}")
    public ResponseEntity<List<DecisionRecordResponse>> listByTicker(@PathVariable String ticker) {
        return ResponseEntity.ok(service.listByTicker(new Ticker(ticker))
                .stream().map(DecisionRecordResponse::from).toList());
    }

    @GetMapping("/ticker/{ticker}/latest")
    public ResponseEntity<DecisionRecordResponse> latestByTicker(@PathVariable String ticker) {
        return service.latestByTicker(new Ticker(ticker))
                .map(DecisionRecordResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/accuracy")
    public ResponseEntity<AccuracyReport> accuracy() {
        return ResponseEntity.ok(service.accuracy());
    }

    @PutMapping("/{id}/outcome")
    public ResponseEntity<Void> recordOutcome(@PathVariable UUID id, @RequestBody OutcomeRequest dto) {
        service.recordOutcome(id, dto.outcome());
        return ResponseEntity.ok().build();
    }
}
