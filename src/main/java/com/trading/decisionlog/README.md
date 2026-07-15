# decisionlog — Karar Günlüğü

Orchestration'ın ürettiği her kararı **değiştirilemez (immutable)** bir kayıt olarak saklar.
Denetim izi (audit trail) ve sonradan sonuç (outcome) değerlendirmesi için kullanılır.

## Sorumluluk
- `DecisionProduced` olayını dinler → `DecisionRecord` olarak kalıcılaştırır.
- Karar sonucunu (CORRECT/INCORRECT/…) sonradan işaretlemeye izin verir.

## Çalışma Şekli
```
DecisionProduced (orchestration)
        ▼
DecisionLogApplicationService.onDecisionProduced
        ▼
DecisionRecord (ticker, action, confidence, reasoning, 4 özet) → decision_records
        ▼
GET /decisions · /decisions/ticker/{ticker}   |   PUT /{id}/outcome
```

## API — `/api/v1/decisions`
| Method | Path | Açıklama |
|---|---|---|
| GET | `` | Tüm kararlar |
| GET | `/ticker/{ticker}` | Hisseye göre kararlar |
| PUT | `/{id}/outcome` | Karar sonucunu kaydet |

## Domain Modelleri
`DecisionRecord`, `DecisionOutcome`.

## Portlar
- **out:** `DecisionRecordRepository`

## Infrastructure
`DecisionRecordEntity` + JPA adapter.

## Kalıcılık
Tablo: `decision_records`.

## Olaylar
- **Tüketir:** `DecisionProduced`
