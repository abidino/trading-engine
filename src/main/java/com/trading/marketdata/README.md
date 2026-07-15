# marketdata — Piyasa Verisi & Teknik Analiz

Fiyat mumlarını (OHLCV) çeker, teknik göstergeleri hesaplar, **günlük trend analizi** yapar ve
**destek/direnç** seviyelerini üretir. Anlık (intraday) fiyat kotasyonu da sağlar.

## Sorumluluk
- Sağlayıcıdan mum verisi senkronlar (`MarketDataProviderPort`), `price_candles`'a yazar.
- Göstergeler: EMA, RSI, MACD → `TechnicalIndicatorSnapshot` / `TechnicalSignal`.
- Trend sınıflandırma: `TrendDirection` (STRONG_UPTREND…STRONG_DOWNTREND).
- Destek/direnç: fractal swing tespiti + kümeleme + floor-trader pivotları.
- Anlık kotasyon (`IntradayQuoteProviderPort`).

## Çalışma Şekli
```
GET /{ticker}/indicators          POST /{ticker}/analyze-trend
        │  loadCandles()→syncCandles() (gerekirse sağlayıcıdan çeker + persist)
        ▼
Domain hesaplayıcılar (EMA/RSI/MACD, SupportResistanceCalculator)
        ▼
TrendAnalysis persist → TechnicalTrendComputed yayınlanır
```
> Not: gösterge/destek-direnç uçları mum senkron-yazımı yaptığı için `readOnly` **değildir**.

## API — `/api/v1/market-data`
| Method | Path | Açıklama |
|---|---|---|
| GET | `/{ticker}/candles` | Mum verisi |
| POST | `/{ticker}/sync` | Mumları senkronla |
| GET | `/{ticker}/indicators?days=` | EMA/RSI/MACD |
| GET | `/{ticker}/support-resistance?days=` | Destek/direnç + pivotlar |
| POST | `/{ticker}/analyze-trend?days=` | Günlük trend hesapla |
| GET | `/{ticker}/trend` · `/trends` | Son / geçmiş trendler |
| POST | `/{ticker}/quote/refresh` | Anlık kotasyon yenile |
| GET | `/{ticker}/quote` · `/quotes` | Anlık kotasyon(lar) |

## Domain Modelleri
`PriceCandle`, `TechnicalIndicatorSnapshot`, `TechnicalSignal`, `TrendAnalysis`,
`TrendDirection`, `SupportResistanceLevels`, `IntradayQuote`, `MarketSession`.

## Portlar
- **in:** `TechnicalAnalysisUseCase`, `IntradayQuoteUseCase`, `MarketDataApplicationService`
- **out:** `MarketDataProviderPort`, `PriceCandleRepository`, `TrendAnalysisRepository`,
  `IntradayQuoteProviderPort`, `IntradayQuoteRepository`

## Infrastructure
- **Sağlayıcı:** `YahooFinanceAdapter`, `YahooIntradayQuoteAdapter` (gerçek);
  `MockMarketDataProvider` (ticker-hash ile sentetik OHLCV), `MockIntradayQuoteProvider`
  (`@Profile("mock")`).
- **JPA:** `PriceCandleEntity`, `TechnicalSignalEntity`, `TrendAnalysisEntity`,
  `IntradayQuoteEntity` + adapter'lar.

## Kalıcılık
Tablolar: `price_candles`, `technical_signals`, `intraday_quotes` (`@Table`) ve
`trend_analysis_entity` (varsayılan adlandırma).

## Olaylar
- **Yayınlar:** `TechnicalTrendComputed` (trend hesaplandığında).
