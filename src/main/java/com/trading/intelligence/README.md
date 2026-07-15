# intelligence — Haber & Sosyal Sinyal

Bir hisse (veya makro/piyasa geneli) hakkında **haberleri toplar**, LLM ile özetler ve
**duygu/anlam** (sentiment) çıkarımı yaparak DB'ye yazar. Sosyal medya sinyallerini de toplar.

## Sorumluluk
- Haber sağlayıcıdan ham haber çeker (`NewsProviderPort`).
- LLM ile her habere `NewsTag` üretir: ticker + sentiment (POSITIVE/NEGATIVE/NEUTRAL) + yorum.
- Ticker `ALL` → makro / piyasa-geneli haber.
- Sosyal sinyalleri toplar ve saklar (`SocialSignalProviderPort`).

## Çalışma Şekli
```
POST /news/scan/{ticker}     POST /news/scan-macro
        │                            │
        ▼                            ▼
NewsProviderPort (NewsAPI | Mock) ──► RawNewsArticle
        ▼
   LLM sınıflandırma ──► NewsArticle + NewsTag[] (sentiment, interpretation)
        ▼
   news_article + news_tag tablolarına yazılır → GET /{ticker}/news
```

## API — `/api/v1/intelligence`
| Method | Path | Açıklama |
|---|---|---|
| GET | `/{ticker}/news?days=` | Bir hissenin haberleri (tag'li) |
| GET | `/news/recent` | Son haberler |
| POST | `/news/scan/{ticker}?limit=` | Hisse haberlerini tara+özetle |
| POST | `/news/scan-macro?limit=` | Makro/piyasa haberlerini tara |
| GET | `/{ticker}/social` | Sosyal sinyaller |
| POST | `/{ticker}/social/collect` | Sosyal sinyal topla |

## Domain Modelleri
`NewsArticle`, `RawNewsArticle`, `NewsTag` (ticker+sentiment+interpretation), `NewsSentiment`,
`NewsCategory`, `SocialSignal`.

## Portlar
- **out:** `NewsProviderPort`, `NewsRepository`, `SocialSignalProviderPort`, `SocialSignalRepository`

## Infrastructure
- **Haber:** `NewsApiAdapter` (gerçek), `MockNewsProvider` (`@Profile("mock")`).
- **Sosyal:** `RedditAdapter` (gerçek), `MockSocialSignalProvider` (`@Profile("mock")`).
- **JPA:** `NewsArticleEntity`, `NewsTagEntity`, `SocialSignalEntity` + adapter'lar.

## Kalıcılık
Tablolar: `social_signals` (`@Table`), `news_article_entity`, `news_tag_entity` (varsayılan
adlandırma stratejisiyle `<Entity>` → snake_case).

## Olaylar
Doğrudan olay yayınlamaz; verisi orchestration tarafından `NewsPort`/`SocialSignalPort` ile okunur.
