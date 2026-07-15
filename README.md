# Trading Engine — Yapay Zeka Destekli Modüler Monolit Trading Motoru

Bir hisse verildiğinde; **haberlerini tarayıp özetleyen ve duygu/anlam çıkaran**, **teknik
analiz + destek/direnç** üreten, **portföy/izleme listesi** yöneten ve tüm bu veriyi bir LLM ile
birleştirip **AL / SAT / TUT** kararına dönüştüren bir motor. Her süreç bağımsız çalışır, UI'dan
tetiklenebilir ve görüntülenebilir; en sonda orkestrasyon hepsini birleştirir.

> **Tek Spring Boot uygulaması, tek `pom.xml`.** Kod domain'e göre klasörlere ayrılmıştır ancak
> Maven çoklu-modül **değildir**. Her domain klasörünün altında kendi `README.md` dosyası vardır
> (çalışma şekli, API'ler, entity'ler, olaylar).

---

## İçindekiler
1. [Genel Bakış](#1-genel-bakış)
2. [Teknoloji Yığını](#2-teknoloji-yığını)
3. [Mimari (Hexagonal + DDD + Olay Tabanlı)](#3-mimari-hexagonal--ddd--olay-tabanlı)
4. [Proje Yapısı](#4-proje-yapısı)
5. [Domain Modülleri](#5-domain-modülleri)
6. [Olay Akışı](#6-olay-akışı)
7. [Uçtan Uca Senaryo](#7-uçtan-uca-senaryo)
8. [Kurulum ve Çalıştırma](#8-kurulum-ve-çalıştırma)
9. [REST API Özeti](#9-rest-api-özeti)
10. [Veritabanı Şeması](#10-veritabanı-şeması)
11. [Web UI (Nuxt)](#11-web-ui-nuxt)
12. [Bilinen Eksikler](#12-bilinen-eksikler)

---

## 1. Genel Bakış

Sistem birbirinden bağımsız çalışabilen **10 domain modülünden** oluşur. Her modül kendi
sorumluluğuna sahiptir ve diğerleriyle **doğrudan bağımlılık yerine domain olayları** üzerinden
haberleşir. Tipik akış:

1. **discovery** — filtrelere göre yeni hisse adayları bulur ve LLM ile değerlendirir.
2. **intelligence** — haber & sosyal sinyalleri toplar, özetler, duygu çıkarır, DB'ye yazar.
3. **marketdata** — fiyat mumlarını çeker, teknik gösterge/trend ve destek/direnç üretir.
4. **portfolio / watchlist** — pozisyon ve izleme listesini yönetir, analiz talep eder.
5. **orchestration** — toplanan tüm veriyi LLM'e verip **karar** üretir (`DecisionProduced`).
6. **decisionlog / notification** — kararı kalıcılaştırır ve uyarı gönderir.
7. **scheduler** — tüm bu işleri cron ile çalıştırır; UI'dan da elle tetiklenebilir.

Her şey **`mock` profili** ile internet/anahtar gerektirmeden uçtan uca çalışır.

---

## 2. Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| Dil / Runtime | **Java 21** |
| Framework | **Spring Boot 4.0.6** (web, validation, data-jpa, mail, scheduling) |
| Boilerplate | **Lombok** |
| JSON | **Jackson 3** (`tools.jackson.databind` — `com.fasterxml` değil) |
| Veritabanı | **PostgreSQL** (gerçek) · **H2** (mock, in-memory) |
| LLM | **Ollama** (varsayılan) · OpenAI · Anthropic (analiz tipine göre yönlendirme) |
| Dış veri | Yahoo Finance · Finviz · NewsAPI · Reddit |
| Port | **4650** |
| UI | **Nuxt 4** (Vue 3, Tailwind) — `ui/` |

---

## 3. Mimari (Hexagonal + DDD + Olay Tabanlı)

Her modül **altıgen (hexagonal / ports & adapters)** yapıdadır:

```
<module>/
├── domain/                 # saf iş mantığı (framework'süz)
│   ├── model/              # value object'ler, entity'ler, enum'lar
│   ├── port/in/            # use-case arayüzleri (uygulamanın sunduğu)
│   ├── port/out/           # repository & dış servis arayüzleri (uygulamanın ihtiyaç duyduğu)
│   └── *ApplicationService # use-case implementasyonu + olay dinleyicileri
├── infrastructure/         # adapter'lar: JPA entity/repo, dış servis istemcileri, mock'lar
└── web/                    # REST controller + DTO'lar
```

İlkeler:
- **Domain, dışarıya bağımlı değildir.** Bağımlılık yönü daima içe doğrudur (adapter → port → domain).
- **Modüller birbirini import etmez.** Ortak dil `shared/kernel`'deki value object'ler ve
  `event/` altındaki domain olaylarıdır. Modüller arası veri gerektiğinde (orchestration'ın diğer
  modüllerden okuması gibi) **ACL adaptörleri** kullanılır.
- **Mock deseni:** her dış servis portunun bir gerçek bir de `@Primary @Profile("mock")` stub
  adaptörü vardır → offline çalışma.

---

## 4. Proje Yapısı

```
src/main/java/com/trading/
├── TradingEngineApplication.java
├── shared/kernel/          → value object'ler, domain olayları, LLM portu   [README]
├── orchestration/          → AI analiz çekirdeği                            [README]
├── discovery/              → hisse keşfi                                    [README]
├── intelligence/           → haber & sosyal sinyal                          [README]
├── marketdata/             → piyasa verisi & teknik analiz                  [README]
├── portfolio/              → portföy yönetimi                               [README]
├── watchlist/              → izleme listesi                                 [README]
├── decisionlog/            → karar günlüğü                                  [README]
├── notification/           → bildirim                                       [README]
├── scheduler/              → zamanlanmış görevler (jobs)                    [README]
├── web/                    → dashboard (toplayıcı okuma katmanı)            [README]
└── config/                 → uygulama yapılandırması                        [README]

src/main/resources/
├── application.yml         → gerçek profil (PostgreSQL, Ollama, dış servisler)
└── application-mock.yml    → offline test harness (H2 + stub'lar)

ui/                         → Nuxt 4 dashboard
```

> Her klasörün altındaki `README.md`, o modülün çalışma şeklini, API'lerini, entity'lerini ve
> olaylarını detaylandırır.

---

## 5. Domain Modülleri

| Modül | Sorumluluk | Ana API kökü | Olaylar (Y=yayınlar, T=tüketir) |
|---|---|---|---|
| **orchestration** | Veriyi toplayıp LLM ile karar üretir | `/api/v1/analysis` | Y: DecisionProduced, AddToWatchlistRecommended · T: AnalysisRequested |
| **discovery** | Filtreyle hisse tarar + değerlendirir | `/api/v1/discovery` | Y: AddToWatchlistRecommended |
| **intelligence** | Haber/sosyal topla, özetle, duygu çıkar | `/api/v1/intelligence` | — |
| **marketdata** | Mum, gösterge, trend, destek/direnç | `/api/v1/market-data` | Y: TechnicalTrendComputed |
| **portfolio** | Pozisyon, işlem, PnL, özet | `/api/v1/portfolio` | Y: AnalysisRequested |
| **watchlist** | İzleme listesi, hedef fiyat, onay | `/api/v1/watchlist` | Y: AnalysisRequested · T: AddToWatchlistRecommended |
| **decisionlog** | Kararların değişmez kaydı | `/api/v1/decisions` | T: DecisionProduced |
| **notification** | Karara göre uyarı gönder | `/api/v1/notifications` | T: DecisionProduced |
| **scheduler** | 6 cron işi + elle tetikleme + log | `/api/v1/scheduler`, `/api/v1/jobs` | (dolaylı) AnalysisRequested |
| **web** (dashboard) | Modüllerden okuyup özetler | `/api/v1/dashboard` | — |
Detaylar için ilgili modülün `README.md` dosyasına bakınız.

---

## 6. Olay Akışı

Modüller Spring `ApplicationEvent` ile gevşek bağlı haberleşir (`shared/kernel/event/`):

```
                         ┌───────────────────────────────────────────┐
 scheduler ─┐            │                                           │
 portfolio ─┤            ▼                                           │
 watchlist ─┼─► AnalysisRequested ─► orchestration ─► DecisionProduced ─┬─► decisionlog
 discovery ─┘                              │                            └─► notification
                                           │
                                           └─► AddToWatchlistRecommended ─► watchlist

 marketdata ─► TechnicalTrendComputed
```

| Olay | Yayınlayan | Tüketen |
|---|---|---|
| `AnalysisRequested` | scheduler, portfolio, watchlist, discovery | orchestration |
| `DecisionProduced` | orchestration | decisionlog, notification |
| `AddToWatchlistRecommended` | discovery (orchestration akışı) | watchlist |
| `TechnicalTrendComputed` | marketdata | ilgili dinleyici |

---

## 7. Uçtan Uca Senaryo

"Bir hisse ver → haberine bak, teknik analizini yap, karar ver" akışının mock ile denenmesi:

```bash
B=http://localhost:4650/api/v1

# 1) Haberleri tara + özetle + duygu çıkar
curl -X POST "$B/intelligence/news/scan/AAPL?limit=5"

# 2) Teknik analiz + trend + destek/direnç
curl -X POST "$B/market-data/AAPL/analyze-trend?days=400"
curl "$B/market-data/AAPL/support-resistance?days=400"

# 3) Tüm veriyi birleştirip karar üret (async)
RID=$(curl -s -X POST "$B/analysis/run" -H 'Content-Type: application/json' \
      -d '{"ticker":"AAPL"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["runId"])')
curl "$B/analysis/status/$RID"     # → completed + action/confidence + 4 özet

# 4) Kararın günlüğü ve uyarısı
curl "$B/decisions"
curl "$B/notifications/alerts"
```

Aynı akış `scheduler` işleriyle otomatik de tetiklenir; ör. `POST /api/v1/jobs/news-scan/trigger`.

---

## 8. Kurulum ve Çalıştırma

### Backend — Mock profili (önerilen, offline)
İnternet, veritabanı veya API anahtarı **gerektirmez**. H2 in-memory + tüm dış servisler için stub.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mock
# Sağlık kontrolü:
curl http://localhost:4650/actuator/health   # {"status":"UP"}
```

### Backend — Gerçek profil
`application.yml` PostgreSQL, Ollama ve dış servisleri bekler (env ile override edilebilir:
`DB_HOST`, `OLLAMA_BASE_URL`, `NEWSAPI_KEY`, `SMTP_*`, vb.).

```bash
mvn spring-boot:run
```

| Ayar | Mock | Gerçek |
|---|---|---|
| DB | H2 in-memory (`create-drop`) | PostgreSQL (`ddl-auto: update`) |
| LLM | `MockLlmAdapter` | Ollama / OpenAI / Anthropic |
| Dış veri | stub adapter'lar | Yahoo / Finviz / NewsAPI / Reddit |
| Mail | devre dışı | SMTP |

### Frontend
```bash
cd ui
npm install
npm run dev        # http://localhost:3000 — API'ye http://localhost:4650 üzerinden bağlanır
npx nuxt typecheck # tip kontrolü
```

---

## 9. REST API Özeti

Tüm uçlar `/api/v1` altındadır. Modül bazlı tam liste ilgili `README.md`'lerdedir.

- **orchestration** `/analysis`: `POST /run`, `GET /status/{runId}`, `GET /runs`, `GET /history/{ticker}`
- **discovery** `/discovery`: `GET /stocks`, `POST /run`, `POST /evaluate/{ticker}`,
  `POST /stocks/{ticker}/promote|dismiss`, `GET|POST /filters`, `POST /filters/{id}/activate|deactivate`
- **intelligence** `/intelligence`: `GET /{ticker}/news`, `POST /news/scan/{ticker}`,
  `POST /news/scan-macro`, `GET /{ticker}/social`
- **marketdata** `/market-data`: `GET /{ticker}/indicators`, `GET /{ticker}/support-resistance`,
  `POST /{ticker}/analyze-trend`, `GET /{ticker}/trends`, `POST /{ticker}/quote/refresh`
- **portfolio** `/portfolio`: `GET /positions`, `/positions/closed`, `/summary`, `POST /transactions`
- **watchlist** `/watchlist`: `GET`, `POST`, `DELETE /{id}`, `PUT /{id}/target-price`,
  `POST /{id}/approve`, `POST /{ticker}/analyze`
- **decisionlog** `/decisions`: `GET`, `GET /ticker/{ticker}`, `PUT /{id}/outcome`
- **notification** `/notifications`: `GET /alerts`
- **scheduler** `/scheduler/status`, `/jobs/recent`, `/jobs/{jobName}`, `POST /jobs/{jobName}/trigger`
- **dashboard** `/dashboard`: `GET /summary`, `/sectors`, `/performance`

---

## 10. Veritabanı Şeması

`ddl-auto` şemayı yönetir (gerçek: `update`, mock: `create-drop`). Ana tablolar:

| Tablo | Modül |
|---|---|
| `discovery_filters`, `discovery_candidate_entity`, `dismissed_ticker_entity` | discovery |
| `news_article_entity`, `news_tag_entity`, `social_signals` | intelligence |
| `price_candles`, `technical_signals`, `intraday_quotes`, `trend_analysis_entity` | marketdata |
| `portfolio_positions`, `portfolio_transactions` | portfolio |
| `watchlist_items` | watchlist |
| `decision_records` | decisionlog |
| `alert_logs` | notification |
| `job_execution_logs` | scheduler |

> `@Table` verilmeyen entity'lerde tablo adı, Spring Boot varsayılan adlandırma stratejisiyle
> `<EntityAdı>` → snake_case (ör. `NewsArticleEntity` → `news_article_entity`) olarak üretilir.

> orchestration kendi tablosunu tutmaz — çalışma durumu bellektedir (`AnalysisRunStore`), kalıcı
> iz `decision_records`'a düşer.

---

## 11. Web UI (Nuxt)

`ui/` altında Nuxt 4 dashboard. Sayfalar her modülle birebir hizalıdır:

`index` (dashboard) · `portfolio` · `watchlist` · `decisions` · `notifications` ·
`intelligence` · `market` · `analysis` · `discovery` · `jobs`

Her sayfa ilgili composable (`ui/app/composables/use*.ts`) üzerinden backend'e bağlanır; API
tabanı `NUXT_PUBLIC_API_BASE_URL` (varsayılan `http://localhost:4650`) ile ayarlanır.

---

## 12. Bilinen Eksikler

- `analystReports` (orchestration sonuç DTO'sunda) şimdilik boş — ileride ajan bazlı raporlarla
  doldurulacak.
- Kimlik doğrulama / yetkilendirme yok (dahili/tek kullanıcılı varsayım).
- Gerçek profildeki dış servis adapter'ları geçerli API anahtarı/erişim gerektirir; doğrulama
  ve dayanıklılık (retry/rate-limit) genişletilebilir.
- Fiziksel Maven çoklu-modül ayrımı bilinçli olarak yapılmadı — mantıksal sınırlar (paket + port
  + olay) ile temiz tutuldu.
