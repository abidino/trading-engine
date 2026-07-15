package com.trading.shared.kernel.llm;

/**
 * Outbound port: the single gateway to any LLM provider.
 *
 * Lives in the shared kernel so EVERY module (marketdata, orchestration, ...)
 * can request LLM completions without depending on the orchestration module.
 *
 * The RoutingLlmAdapter (orchestration.infrastructure) is @Primary and delegates to the
 * correct provider based on LlmRequest.analysisType() and application.yml routing config.
 */
public interface LlmPort {
    LlmResponse complete(LlmRequest request);
}
