package com.trading.watchlist.web;

import jakarta.validation.constraints.NotBlank;

public record AddItemRequest(@NotBlank String ticker) {}
