# notification — Bildirim

Üretilen kararlara göre **uyarı** gönderir (ör. e-posta) ve gönderim kaydını tutar.

## Sorumluluk
- `DecisionProduced` olayını dinler → yapılandırılabilir kurallara göre uyarı üretir.
- Gönderimi `NotificationDeliveryPort` üzerinden yapar, sonucu `AlertLog` olarak saklar.

## Çalışma Şekli
```
DecisionProduced (orchestration)
        ▼
NotificationApplicationService.onDecisionProduced
        ▼
NotificationDeliveryPort (Email | Mock) ──► AlertLog (SENT/SKIPPED/FAILED) → alert_logs
        ▼
GET /notifications/alerts
```

## API — `/api/v1/notifications`
| Method | Path | Açıklama |
|---|---|---|
| GET | `/alerts` | Gönderilen/atlanan uyarılar |

## Domain Modelleri
`AlertLog`, `AlertChannel` (EMAIL…), `DeliveryStatus` (SENT/SKIPPED/FAILED).

## Portlar
- **out:** `NotificationDeliveryPort`, `AlertLogRepository`

## Infrastructure
- `EmailNotificationAdapter` (gerçek, SMTP), `MockNotificationDelivery` (`@Profile("mock")`).
- `AlertLogEntity` + JPA adapter.

## Kalıcılık
Tablo: `alert_logs`.

## Olaylar
- **Tüketir:** `DecisionProduced`
