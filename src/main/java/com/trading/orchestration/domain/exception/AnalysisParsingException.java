package com.trading.orchestration.domain.exception;

/** Thrown when the LLM response cannot be parsed into a valid AnalysisResult. */
public class AnalysisParsingException extends RuntimeException {
    public AnalysisParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
