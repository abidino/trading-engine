# config — Uygulama Yapılandırması

Uygulama genelindeki teknik yapılandırma (domain mantığı içermez).

## İçerik
- `WebConfig` — CORS ve web katmanı ayarları (UI'nin `http://localhost:3000` gibi origin'den
  API'ye erişebilmesi için).

## Notlar
- Profil bazlı ayarlar `src/main/resources/application.yml` (gerçek) ve
  `application-mock.yml` (offline test harness) dosyalarındadır.
- `mock` profili: H2 in-memory DB + tüm dış servisler için stub adaptörler → internet/anahtar
  gerektirmeden tüm modüller çalışır.
