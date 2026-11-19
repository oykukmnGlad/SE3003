# ReserveStudio Backend (Spring Boot + MySQL)

Campus Studio & Equipment Booking System — backend (Ahmet Efe Yavuz). Veritabanı şeması Mete'nin
`Db.txt` dosyasıdır (`db/reservestudio_db.sql`).

## 1. Gereksinimler
- JDK 17 veya üstü
- Maven 3.9+
- MySQL 8+

## 2. Veritabanını hazırla (bir kez)
```bash
mysql -u root -p < db/reservestudio_db.sql
```
Veya MySQL Workbench'te `db/reservestudio_db.sql` dosyasını açıp çalıştırın. `reservestudio_db` veritabanı
ve `users`, `slots`, `bookings` tabloları oluşur. Hibernate tablolara dokunmaz (`ddl-auto=none`);
şemanın sahibi SQL dosyasıdır.

## 3. Bağlantı ayarları
Varsayılan: `localhost:3306/reservestudio_db`, kullanıcı `root`, şifre boş.
Kendi MySQL şifrenizi ortam değişkeniyle verin (dosyaya şifre yazmayın, GitHub'a gitmesin):
```bash
export DB_USER=root
export DB_PASSWORD='mysql-sifreniz'
# gerekirse: export DB_URL='jdbc:mysql://localhost:3306/reservestudio_db?serverTimezone=Europe/Istanbul&useSSL=false&allowPublicKeyRetrieval=true'
```

## 4. Çalıştır
```bash
mvn spring-boot:run
```
Uygulama `http://localhost:8080` adresinde açılır. Açılışta bugünden itibaren 30 günlük saatlik
slotlar (09:00–20:00 başlangıçlı) `slots` tablosuna otomatik yazılır.

## 5. Testler
```bash
mvn test
```
Testler H2 (bellekte, MySQL modu) kullanır; MySQL gerekmez. `test-schema.sql`, `db/reservestudio_db.sql` ile aynı
tabloları içerir. `ConcurrentBookingTest`, aynı slota 20 eş zamanlı istek atar ve yalnızca 1'inin başarılı olduğunu doğrular.

## 6. API
| Metot | URL | Açıklama |
| --- | --- | --- |
| POST | `/api/auth/signup` | `{"username","email","password"}` → 201 |
| POST | `/api/auth/login` | `{"email","password"}` → 200 + oturum cookie'si (`JSESSIONID`) |
| POST | `/api/auth/logout` | 204 |
| GET | `/api/auth/me` | Giriş yapmış kullanıcı (giriş gerekir) |
| GET | `/api/slots?date=2026-11-23` | Günün slotları: `[{id,start,end,available}]` (giriş gerekmez) |
| POST | `/api/bookings` | `{"slotId":123}` → 201; slot doluysa **409** (giriş gerekir) |
| GET | `/api/bookings/my` | Kendi rezervasyonlarım |
| DELETE | `/api/bookings/{id}` | Rezervasyonu iptal et (yalnızca sahibi) → 204 |

Hata formatı: `{"status":409,"error":"Conflict","message":"Bu slot zaten rezerve edilmiş","timestamp":"..."}`

Hızlı deneme (curl, oturumu cookie dosyasında tutar):
```bash
curl -c c.txt -H 'Content-Type: application/json' -d '{"username":"efe","email":"efe@test.com","password":"12345678"}' localhost:8080/api/auth/signup
curl -c c.txt -b c.txt -H 'Content-Type: application/json' -d '{"email":"efe@test.com","password":"12345678"}' localhost:8080/api/auth/login
curl "localhost:8080/api/slots?date=$(date +%F)"
curl -b c.txt -H 'Content-Type: application/json' -d '{"slotId":1}' localhost:8080/api/bookings
```

## 7. Çakışma (race condition) savunması
`POST /api/bookings` iki katmanlı çalışır:
1. Atomik `UPDATE slots SET is_available=false WHERE id=? AND is_available=true`: aynı anda gelen isteklerden yalnızca biri 1 satır günceller.
2. `bookings.slot_id` üzerindeki `UNIQUE` kısıtı (`unique_slot_booking`): ilk adım aşılsa bile veritabanı ikinci kaydı reddeder, transaction geri alınır, istemciye 409 döner.

İptalde rezervasyon satırı silinir ve slot tekrar `is_available=true` olur (`UNIQUE(slot_id)` yüzünden `CANCELLED` satırı tutulamaz).

## 8. Frontend (Öykü) için notlar
- Oturum cookie tabanlıdır: fetch/axios isteklerinde `credentials: 'include'` (axios: `withCredentials: true`) kullanılmalı.
- CORS'a izinli adresler `application.properties` içindeki `app.cors.allowed-origins` değerindedir (varsayılan 3000 ve 5173).
