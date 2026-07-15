# watchlist — İzleme Listesi

İlgilenilen hisseleri takip eder. Discovery'den **promote** edilen adaylar buraya düşer; her
kalem için hedef fiyat, onay ve analiz talebi yönetilir.

## Sorumluluk
- İzleme kalemi ekle (yalnızca ticker), sil, hedef fiyat güncelle, **onayla**.
- Kalem için analiz talep et → `AnalysisRequested (WATCHLIST_REVIEW)` yayınlar.
- Discovery'nin `AddToWatchlistRecommended` olayını tüketip otomatik ekleme yapar.

## Çalışma Şekli
```
POST /watchlist {ticker}         AddToWatchlistRecommended (discovery)
        │                                  │
        ▼                                  ▼
        └──────►  WatchlistItem  ◄─────────┘
        ▼
POST /{ticker}/analyze ──► AnalysisRequested(WATCHLIST_REVIEW) ──► orchestration
```

## API — `/api/v1/watchlist`
| Method | Path | Açıklama |
|---|---|---|
| GET | `` | İzleme listesi |
| POST | `` | Ekle `{ticker}` |
| DELETE | `/{id}` | Çıkar |
| PUT | `/{id}/target-price` | Hedef fiyat `{price}` |
| POST | `/{id}/approve` | Onayla |
| POST | `/{ticker}/analyze` | Analiz iste |

## Domain Modelleri
`WatchlistItem` (ticker, addedAt, targetPrice, stopLoss, notes, approved).

## Portlar
- **in:** `WatchlistUseCase`
- **out:** `WatchlistRepository`

## Infrastructure
`WatchlistItemEntity` + JPA adapter.

## Kalıcılık
Tablo: `watchlist_items`.

## Olaylar
- **Tüketir:** `AddToWatchlistRecommended`
- **Yayınlar:** `AnalysisRequested` (WATCHLIST_REVIEW)
