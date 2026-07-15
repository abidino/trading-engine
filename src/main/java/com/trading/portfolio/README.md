# portfolio — Portföy Yönetimi

Alım/satım işlemlerini kaydeder, **açık pozisyonları** ve kapanmış (satılmış) pozisyonları
türetir, kâr/zarar (PnL) ve portföy özetini hesaplar.

## Sorumluluk
- İşlem (BUY/SELL) kaydı → pozisyonların yeniden hesaplanması.
- Açık pozisyon: ortalama maliyet, güncel değer, gerçekleşmemiş PnL.
- Portföy özeti: toplam değer, PnL, komisyon, kazanan/kaybeden sayıları.
- Pozisyon için analiz talep edebilir (`AnalysisRequested` yayınlar).

## Çalışma Şekli
```
POST /transactions (BUY/SELL)
      ▼
PortfolioApplicationService  ──►  PortfolioPosition yeniden hesaplanır (avg cost, PnL)
      ▼
GET /positions · /positions/closed · /summary
```

## API — `/api/v1/portfolio`
| Method | Path | Açıklama |
|---|---|---|
| GET | `/positions` · `/positions/closed` | Açık / kapanmış pozisyonlar |
| GET | `/transactions` | İşlem geçmişi |
| GET | `/summary` | Portföy özeti (değer, PnL, komisyon) |
| POST | `/transactions` | İşlem kaydet |
| PUT | `/positions/{positionId}/stop-loss` | Stop-loss güncelle |
| POST | `/positions/{ticker}/analyze` | Pozisyon için analiz iste |

## Domain Modelleri
`PortfolioPosition`, `PortfolioTransaction`, `TransactionType` (BUY/SELL).

## Portlar
- **in:** `PortfolioUseCase`
- **out:** `PortfolioPositionRepository`, `PortfolioTransactionRepository`

## Infrastructure
`PortfolioPositionEntity`, `PortfolioTransactionEntity` + JPA adapter'ları. (Mock gerektirmez —
saf hesaplama + kalıcılık.)

## Kalıcılık
Tablolar: `portfolio_positions`, `portfolio_transactions`.

## Olaylar
- **Yayınlar:** `AnalysisRequested` (pozisyon analizi talebiyle).
