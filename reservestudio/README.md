# ReserveStudio — Campus Studio & Equipment Booking System

Tek klasörde iki parça:

```
reservestudio/
├── backend/    Spring Boot + MySQL (REST API)       -> Ahmet Efe
│   └── db/     reservestudio_db.sql (Mete'nin şeması)
└── frontend/   Düz HTML/CSS/JS arayüz                -> Öykü
```

Arayüz derleme sırasında backend'e dahil edilir; **tek komutla** hem API hem arayüz `http://localhost:8080`
adresinden çalışır (aynı origin olduğu için CORS/cookie sorunu yaşanmaz).

## 1. Gereksinimler
JDK 17+, Maven 3.9+, MySQL 8+. (macOS: `brew install --cask temurin@17 && brew install maven mysql`)

## 2. Veritabanı (bir kez)
```bash
brew services start mysql
mysql -u root -p < backend/db/reservestudio_db.sql      # şifre yoksa -p'yi kaldırın
mysql -u root -p -e "USE reservestudio_db; SHOW TABLES;" # bookings, slots, users görünmeli
```

## 3. Çalıştır
```bash
cd backend
export DB_PASSWORD='mysql-sifreniz'     # MySQL root şifresi yoksa gerek yok
mvn test                                # önce testler: BUILD SUCCESS görmelisiniz
mvn spring-boot:run
```
Tarayıcıda **http://localhost:8080** adresini açın (`127.0.0.1` yerine `localhost` kullanın).
Kayıt ol → slot seç → rezervasyon yap → "My Bookings" sayfasından iptal et.

Açılışta bugünden itibaren 30 günlük saatlik slotlar (09:00–20:00) `slots` tablosuna otomatik yazılır
(31 gün × 12 = 372 satır). Oturumlar bellekte tutulur: backend yeniden başlayınca herkes çıkış yapmış olur.

### Arayüzü ayrı sunucudan açmak (Öykü için)
Arayüz üzerinde çalışırken backend'i yeniden başlatmadan değişiklikleri görmek için `frontend/` klasörünü
ayrı bir sunucudan açabilirsiniz; backend 8080'de çalışmaya devam eder:
```bash
cd frontend && python3 -m http.server 5500     # sonra http://localhost:5500
```
VS Code **Live Server** eklentisi de çalışır (`http://127.0.0.1:5500` veya `http://localhost:5500`).
`api.js` backend adresini sayfanın adresinden bulur; `localhost` ve `127.0.0.1` her portta CORS'ta izinlidir.
**`index.html` dosyasına çift tıklayıp `file://` ile açmayın**: tarayıcı oturum cookie'sini ve CORS'u engeller,
"Cannot reach the server" görürsünüz.

Arayüz dosyalarını değiştirdikten sonra backend'den görmek için `mvn spring-boot:run` komutunu yeniden başlatın.

## 4. API
| Metot | URL | Açıklama |
| --- | --- | --- |
| POST | `/api/auth/signup` | `{"username","email","password"}` → 201 |
| POST | `/api/auth/login` | `{"email","password"}` → 200 + `JSESSIONID` cookie'si |
| POST | `/api/auth/logout` | 204 |
| GET | `/api/auth/me` | Giriş yapmış kullanıcı (giriş gerekir, yoksa 401) |
| GET | `/api/slots?date=2026-11-23` | `[{id,start,end,available}]` (giriş gerekmez, en fazla 30 gün ilerisi) |
| POST | `/api/bookings` | `{"slotId":123}` → 201; slot doluysa **409** (giriş gerekir) |
| GET | `/api/bookings/my` | Kendi rezervasyonlarım (başlangıca göre artan) |
| DELETE | `/api/bookings/{id}` | İptal (yalnızca sahibi) → 204 |

Hata formatı: `{"status":409,"error":"Conflict","message":"Bu slot zaten rezerve edilmiş","timestamp":"..."}`
(arayüz `message` alanını ekranda gösterir). Fetch/axios isteklerinde `credentials: 'include'` kullanılmalı.

## 5. Çakışma (race condition) savunması — R-01 / WP-03
`POST /api/bookings` iki katmanlı çalışır:
1. Atomik `UPDATE slots SET is_available=false WHERE id=? AND is_available=true`: aynı anda gelen isteklerden yalnızca biri 1 satır günceller.
2. `bookings.slot_id` üzerindeki `UNIQUE` kısıtı (`unique_slot_booking`): ilk adım aşılsa bile veritabanı ikinci kaydı reddeder; istemciye 409 döner.

İptalde rezervasyon satırı silinir ve slot tekrar müsait olur (`UNIQUE(slot_id)` yüzünden `CANCELLED` satırı tutulamaz).

Gerçek MySQL'de eş zamanlı deneme (uygulama çalışırken, giriş yaptıktan sonra, boş bir slot id'siyle):
```bash
seq 20 | xargs -P 20 -I{} curl -s -o /dev/null -w "%{http_code}\n" -b c.txt -H 'Content-Type: application/json' -d '{"slotId":SLOT_ID}' localhost:8080/api/bookings
# beklenen: bir tane 201, on dokuz tane 409
```

## 6. Testler
`mvn test` (backend klasöründe) H2 ile çalışır, MySQL gerekmez:
- `AuthAndBookingApiTest`: kayıt, giriş, oturum, rezervasyon, 409, iptal, yetki kontrolleri
- `ConcurrentBookingTest`: aynı slota 20 eş zamanlı istek, yalnızca 1 başarılı
- `FrontendIntegrationTest`: CORS (Live Server vb. adresler, yabancı origin reddi), 404 davranışı, arayüz sayfalarının sunulması

## 7. Sık çıkan hatalar
| Hata | Çözüm |
| --- | --- |
| `Access denied for user 'root'` | `export DB_PASSWORD='...'` |
| `Communications link failure` | `brew services start mysql` |
| `Unknown database 'reservestudio_db'` | 2. adımdaki SQL dosyasını çalıştırın |
| `Port 8080 was already in use` | Eski uygulamayı `Ctrl+C` ile kapatın |
| Sayfada "Cannot reach the server" | Backend çalışmıyor ya da sayfa `file://` ile açıldı |
| Giriş yaptım ama sürekli girişe atıyor | Sayfayı `127.0.0.1:8080` ile açtıysanız `localhost:8080` kullanın; backend yeniden başladıysa tekrar giriş yapın |
