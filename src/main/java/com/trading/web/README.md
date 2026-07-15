# web — Dashboard (Toplayıcı Okuma Katmanı)

Modüllere yayılmış veriyi **tek ekranda özetleyen** salt-okunur dashboard uçları. Kendi domain'i
yoktur; diğer modüllerin use-case'lerinden okuyup birleştirir (read-model / BFF benzeri).

## Sorumluluk
- Portföy, watchlist, son kararlar, sektör dağılımı ve performans özetini tek yanıtta toplar.

## API — `/api/v1/dashboard`
| Method | Path | Açıklama |
|---|---|---|
| GET | `/summary` | Genel özet (`DashboardSummary`) |
| GET | `/sectors` | Sektör dağılımı (`SectorBreakdown`) |
| GET | `/performance` | Performans (`PerformanceEntry`, `TickerPnl`) |

## DTO'lar
`DashboardSummary`, `SectorBreakdown`, `PerformanceEntry`, `TickerPnl`.

> Not: Buradaki controller yalnızca **okuma/aggregation** yapar; iş kuralları ilgili modüllerde
> kalır. Yeni bir alan gerektiğinde ilgili modülün use-case'i genişletilir, mantık buraya taşınmaz.
