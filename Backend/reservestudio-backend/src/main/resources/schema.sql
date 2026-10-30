-- ReserveStudio MySQL şeması (ortak kaynak: Mete ile birlikte netleştirilebilir)

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS bookings (
    id         BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    studio_id  BIGINT   NOT NULL DEFAULT 1,
    user_id    BIGINT   NOT NULL,
    slot_start DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Çift rezervasyonu veritabanı seviyesinde engelleyen asıl kısıt:
    CONSTRAINT uk_booking_studio_slot UNIQUE (studio_id, slot_start),
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_booking_user (user_id)
);
