# scheduler — Zamanlanmış Görevler (Jobs)

6 arka plan işini cron ile çalıştırır; her biri UI'dan **elle tetiklenebilir** ve her çalışma
loglanır. İşlerin çoğu ilgili modül use-case'ini çağırır veya `AnalysisRequested` yayınlar.

## İşler (Jobs)
| Kanonik ad | Sınıf | Varsayılan cron | Yaptığı iş |
|---|---|---|---|
| `discovery` | DiscoveryJob | `0 0 7 * * MON-FRI` | Aktif filtrelerle tarama+değerlendirme |
| `portfolio-analysis` | PortfolioAnalysisJob | `0 30 6 * * MON-FRI` | Pozisyonlar için analiz |
| `watchlist-analysis` | WatchlistAnalysisJob | `0 0 7 * * MON-FRI` | İzleme listesi için analiz |
| `technical-trend` | TechnicalTrendJob | `0 30 21 * * MON-FRI` | Borsa kapanışı sonrası trend |
| `news-scan` | NewsScanJob | `0 0 * * * *` | Haber tarama+özet |
| `intraday-quote` | IntradayQuoteJob | `0 0/10 * * * *` | Anlık kotasyon yenileme |

> **Önemli:** Her işin loglanan `jobName`'i, status/trigger uçlarındaki **kanonik kebab-case**
> ad ile aynıdır (ör. `discovery`) — böylece status / trigger / recent / history tek kimlikte.

## Çalışma Şekli
```
@Scheduled(cron) ──┐                POST /jobs/{name}/trigger ──┐
                   ▼                                            ▼
             Job.run() ──► JobExecutionLog.start(name) → ilgili use-case → complete/fail
                   ▼
             job_execution_logs → GET /jobs/recent, GET /jobs/{name}
```

## API
| Method | Path | Açıklama |
|---|---|---|
| GET | `/api/v1/scheduler/status` | Kayıtlı işler + çözülmüş cron ifadeleri |
| GET | `/api/v1/jobs/recent?limit=` | Son çalışmalar (tüm işler) |
| GET | `/api/v1/jobs/{jobName}?limit=` | Bir işin geçmişi (kebab ad) |
| POST | `/api/v1/jobs/{jobName}/trigger` | İşi elle tetikle |

## Domain Modelleri
`JobExecutionLog`, `JobStatus` (RUNNING/SUCCESS/FAILED).

## Portlar
- **out:** `JobExecutionLogRepository`

## Infrastructure
`JobExecutionLogEntity` + JPA adapter. `SchedulerController` cron değerlerini `@Value` ile
enjekte eder.

## Kalıcılık
Tablo: `job_execution_logs`.

## Olaylar
İşler ilgili use-case'leri çağırır; analiz gerektirenler dolaylı olarak `AnalysisRequested`
akışını tetikler.
