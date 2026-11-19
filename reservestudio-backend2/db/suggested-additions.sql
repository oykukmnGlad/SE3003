-- ÖNERİ (zorunlu değil): Mete ile konuşup şemaya eklenebilir. Uygulama bunlar olmadan da çalışır.
USE reservestudio_db;

-- Aynı başlangıç saatli iki slot oluşmasını veritabanı seviyesinde engeller.
-- (Uygulama slotları kendisi üretiyor; tek sunucu çalıştığı sürece çift slot oluşmaz,
--  ama bu kısıt birden fazla sunucuya geçilirse de güvence verir.)
-- Not: slots tablosunda zaten çift kayıt varsa önce onları silmek gerekir.
ALTER TABLE slots ADD CONSTRAINT uk_slots_start_time UNIQUE (start_time);

-- Kullanıcının rezervasyonlarını listelemeyi hızlandırır.
CREATE INDEX idx_bookings_user ON bookings (user_id);
