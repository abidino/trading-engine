# shared / kernel — Paylaşılan Çekirdek

Tüm modüllerin ortak kullandığı **değer nesneleri (value objects)**, **domain olayları** ve
**LLM portu**. Buraya yalnızca gerçekten paylaşılan, kararlı (stable) tipler konur — modüller
arası doğrudan bağımlılık yerine bu çekirdek + olaylar kullanılır.

## İçerik

### Value Objects
- `Ticker` — hisse sembolü (doğrulanmış).
- `Money` — para birimi + miktar.
- `TradeDate` / `AssetType` / `AnalysisRequestType` — ortak enum/tipler.

### Domain Olayları (`event/`)
Modüller arası iletişim Spring `ApplicationEvent` ile **gevşek bağlı (loosely coupled)** yapılır:

| Olay | Yayınlayan | Tüketen |
|---|---|---|
| `AnalysisRequested` | scheduler, portfolio, watchlist, discovery | orchestration |
| `DecisionProduced` | orchestration | decisionlog, notification |
| `AddToWatchlistRecommended` | discovery (orchestration akışı) | watchlist |
| `TechnicalTrendComputed` | marketdata | (dinleyen ilgili modül) |

`DomainEvent` tüm olayların ortak arayüzüdür.

### LLM Portu (`llm/`)
- `LlmPort` — LLM soyutlaması (çıkış portu).
- `LlmRequest` / `LlmResponse` — istek/yanıt sözleşmesi.
- Gerçek uygulamalar orchestration modülündedir (Ollama/OpenAI/Anthropic + Routing + Mock).

## Kural
Bu paket **hiçbir modüle bağımlı değildir**; herkes buna bağımlıdır. Modüller birbirini doğrudan
import etmez — ortak dil buradaki tipler ve olaylardır.
