package com.trading.orchestration.domain.port.in;

import com.trading.orchestration.domain.model.AnalysisRequest;
import com.trading.orchestration.domain.model.AnalysisResult;

/**
 * Inbound port: trigger an AI analysis and wait for the result synchronously.
 * Implemented by AnalysisApplicationService.
 * Called by REST controller and consumed internally via event listener.
 */
public interface RequestAnalysisUseCase {
    AnalysisResult analyze(AnalysisRequest request);
}
