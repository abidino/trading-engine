package com.trading.portfolio.web;

import com.trading.portfolio.domain.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank String ticker, @NotNull TransactionType transactionType,
        @NotNull @Positive BigDecimal quantity, @NotNull @Positive double price, Double commission
) {}
