package com.trading.orchestration.web;

import com.trading.shared.kernel.AnalysisRequestType;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record AnalysisRequestDto(
        @NotBlank String ticker,
        AnalysisRequestType requestType,
        Map<String, String> contextMetadata
) {}
