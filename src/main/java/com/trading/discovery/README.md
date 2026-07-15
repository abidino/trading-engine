# discovery — Hisse Keşfi

Kaydedilmiş filtre kriterlerine göre (Finviz tarzı) yeni hisse **adayları** bulur ve her adayı
LLM ile değerlendirip **önerilir/önerilmez** olarak işaretler. Kendi kendine yeter — tarama +
değerlendirmeyi tek geçişte yapar.

## Sorumluluk
- Aktif `SavedFilter`'ları çalıştırıp aday hisseleri tarar (`StockScreenerPort`).
- Her adayı LLM ile değerlendirir (skor, güven, gerekçe, trend yönü).
- Aday → izleme listesine **promote** veya **dismiss** akışlarını yönetir.
- Önerilen adaylar için `AddToWatchlistRecommended` tetikleyebilir (orchestration üzerinden).

## Çalışma Şekli
```
POST /run
   │  aktif SavedFilter'lar
   ▼
StockScreenerPort (Finviz | Mock) ──► PotentialStock listesi
   ▼
her aday için LLM değerlendirmesi ──► DiscoveryCandidate (RECOMMENDED / NOT_RECOMMENDED)
   ▼
GET /stocks ile UI'da listelenir → promote (watchlist) veya dismiss
```

## API — `/api/v1/discovery`
| Method | Path | Açıklama |
|---|---|---|
| GET | `/stocks` | Değerlendirilmiş adaylar |
| GET | `/candidates` | Ham adaylar |
| POST | `/run` | Aktif kayıtlı filtrelerle tarama + değerlendirme → `{newStocksFound}` |
| POST | `/run/ad-hoc` | Gövdedeki kriterlerle **anlık** tarama (kaydetmeden) → değerlendirilen aday listesi |
| POST | `/evaluate/{ticker}` | Tek hisseyi değerlendir |
| POST | `/stocks/{ticker}/promote` | İzleme listesine ekle |
| POST | `/stocks/{ticker}/dismiss` | Reddet |
| GET/DELETE | `/dismissed`, `/dismissed/{ticker}` | Reddedilenler |
| GET/POST | `/filters` | Filtre listele / oluştur |
| POST | `/filters/{id}/activate` · `/deactivate` | Filtre aç/kapa |
| DELETE | `/filters/{id}` | Filtre sil |

## Domain Modelleri
`DiscoveryCandidate`, `DiscoveryFilter` (kriterler), `SavedFilter`, `PotentialStock`,
`DiscoveryStatus` (NEW/RECOMMENDED/NOT_RECOMMENDED/PROMOTED/DISMISSED).

## Portlar
- **in:** `DiscoveryUseCase`
- **out:** `StockScreenerPort`, `DiscoveryCandidateRepository`, `SavedFilterRepository`,
  `DismissedTickerRepository`

## Infrastructure
- **Screener:** `FinvizScreenerAdapter` (gerçek — **ücretsiz/anahtarsız** Finviz screener HTML'i
  `finviz.com/screener.ashx?v=111` parse eder; opsiyonel `finviz.auth-token` verilirse Elite olarak
  daha fazla satır çeker), `MockStockScreener` (`@Profile("mock")`, AAPL/MSFT/NVDA/AMD/GOOGL döner).
- **JPA:** `DiscoveryCandidateEntity`, `SavedFilterEntity`, `DismissedTickerEntity` + adapter'lar.

## Kalıcılık
Tablolar: `discovery_filters` (`@Table`), `discovery_candidate_entity`,
`dismissed_ticker_entity` (varsayılan adlandırma). `ddl-auto` şemayı yönetir.

## Olaylar
- **Yayınlar:** `AddToWatchlistRecommended` (önerilen adaylar için, orchestration akışıyla).
