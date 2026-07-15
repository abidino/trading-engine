# orchestration — AI Analiz Çekirdeği

Projenin beynidir. Bir hisse için toplanmış **tüm veriyi** (teknik, temel, haber, sosyal)
bir araya getirir, LLM'e verir ve **AL / SAT / TUT / İZLEME-LİSTESİNE-EKLE** kararı üretir.

## Sorumluluk
- `AnalysisRequested` olayını dinler (scheduler / portfolio / watchlist / discovery yayınlar).
- Diğer modüllerden **outbound port**'lar üzerinden veri çeker (doğrudan bağımlılık yok — ACL).
- LLM'e gönderir, sonucu `AnalysisResult`'a dönüştürür.
- `DecisionProduced` olayını yayınlar (decisionlog + notification tüketir).
- Karar `ADD_TO_WATCHLIST` ise `AddToWatchlistRecommended` yayınlar (watchlist tüketir).

## Çalışma Şekli (akış)
```
AnalysisRequested
      │
      ▼
AnalysisApplicationService.onAnalysisRequested
      │  TechnicalDataPort / TechnicalTrendPort
      │  FundamentalDataPort / NewsPort / SocialSignalPort
      ▼
   AnalysisContext  ──►  LlmPort (Routing → Ollama/OpenAI/Anthropic | Mock)
      ▼
   AnalysisResult (action, confidence, reasoning, 4 özet)
      ▼
   DecisionProduced ─► decisionlog (kalıcı kayıt) + notification (uyarı)
```
UI için `POST /run` bu akışı **async** başlatır; `AnalysisRunStore` bellekte durum tutar,
UI `GET /status/{runId}` ile `running → completed/failed` durumunu poll eder.

## API — `/api/v1/analysis`
| Method | Path | Açıklama |
|---|---|---|
| POST | `/run` | Async analiz başlat `{ticker}` → `{runId}` |
| GET | `/status/{runId}` | Çalışma durumu + sonuç |
| GET | `/runs` | Tüm çalışmalar |
| GET | `/history/{ticker}` | Bir hisseye ait çalışmalar |
| POST | `/` | Senkron doğrudan analiz (programatik) |

## Domain Modelleri
`AnalysisRequest`, `AnalysisContext`, `AnalysisResult`, `AnalysisAction` (BUY/SELL/HOLD/WAIT/
ADD_TO_WATCHLIST/REMOVE/IGNORE), `TechnicalData`, `FundamentalData`.

## Portlar
- **in:** `RequestAnalysisUseCase`
- **out:** `TechnicalDataPort`, `TechnicalTrendPort`, `FundamentalDataPort`, `NewsPort`,
  `SocialSignalPort`

## Infrastructure
- **LLM:** `RoutingLlmAdapter` (`@Profile("!mock")`, analiz tipine göre sağlayıcı seçer) →
  `OllamaLlmAdapter` / `OpenAiLlmAdapter` / `AnthropicLlmAdapter`; `LlmProperties`.
- **ACL adaptörleri:** `TechnicalDataAdapter`, `TechnicalTrendAdapter`, `FundamentalDataAdapter`,
  `NewsIntelligenceAdapter`, `SocialIntelligenceAdapter` (diğer modüllerin use-case'lerini sarar).
- **Mock:** `MockLlmAdapter`, `MockFundamentalDataProvider` (`@Profile("mock")`).

## Kalıcılık
Kendi DB tablosu **yoktur** — çalışma durumu `AnalysisRunStore` içinde bellekte tutulur.
Kalıcı iz `DecisionProduced` üzerinden `decisionlog`'a düşer.

## Olaylar
- **Tüketir:** `AnalysisRequested`
- **Yayınlar:** `DecisionProduced`, `AddToWatchlistRecommended`
