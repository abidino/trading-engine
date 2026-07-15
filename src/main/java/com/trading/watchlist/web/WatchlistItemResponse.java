package com.trading.watchlist.web;

import com.trading.watchlist.domain.model.WatchlistItem;

import java.util.UUID;

public record WatchlistItemResponse(UUID id, String ticker, String addedAt,
                                    String targetPrice, String stopLoss, String notes, boolean approved) {
    public static WatchlistItemResponse from(WatchlistItem i) {
        return new WatchlistItemResponse(i.getId(), i.getTicker().value(), i.getAddedAt().toString(),
                i.getTargetPrice() != null ? i.getTargetPrice().toString() : null,
                i.getStopLoss() != null ? i.getStopLoss().toString() : null,
                i.getNotes(), i.isApproved());
    }
}
