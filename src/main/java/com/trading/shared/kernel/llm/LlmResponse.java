package com.trading.shared.kernel.llm;

import java.time.Duration;

/**
 * Outbound port DTO: response from the LLM.
 */
public record LlmResponse(
        String content,
        String modelUsed,
        int tokensUsed,
        Duration latency
) {}
