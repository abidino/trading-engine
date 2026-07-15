package com.trading.intelligence.web;

import com.trading.intelligence.domain.model.SocialSignal;

public record SocialResponse(String ticker, String source, String content,
                              double engagementScore, double sentimentScore) {
    public static SocialResponse from(SocialSignal s) {
        return new SocialResponse(s.ticker(), s.source(), s.content(),
                s.engagementScore(), s.sentimentScore());
    }
}
