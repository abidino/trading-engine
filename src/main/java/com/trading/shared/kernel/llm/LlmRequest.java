package com.trading.shared.kernel.llm;

import com.trading.shared.kernel.AnalysisRequestType;

/**
 * Outbound port DTO: request to the LLM.
 * Defined in the shared kernel so any domain service can construct requests
 * without depending on any infrastructure or another module.
 */
public record LlmRequest(
        String systemPrompt,
        String userPrompt,
        /** Optional model override — null means use provider default. */
        String modelOverride,
        double temperature,
        int maxTokens,
        AnalysisRequestType analysisType
) {
    public static LlmRequest of(String systemPrompt, String userPrompt, AnalysisRequestType type) {
        return new LlmRequest(systemPrompt, userPrompt, null, 0.7, 2048, type);
    }

    public static LlmRequest withModel(String systemPrompt, String userPrompt,
                                       String model, AnalysisRequestType type) {
        return new LlmRequest(systemPrompt, userPrompt, model, 0.7, 2048, type);
    }
}
