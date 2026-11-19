package com.reservestudio.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Tablo: bookings (id, user_id, slot_id, status, created_at).
 * DB'deki UNIQUE (slot_id) kısıtı (unique_slot_booking) bir slotun iki kez alınmasını
 * veritabanı seviyesinde engeller. Aşağıdaki @UniqueConstraint yalnızca bu kuralı
 * entity'de de belgeler (şemayı oluşturan biz değiliz, ddl-auto=none).
 */
@Entity
@Table(name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "unique_slot_booking", columnNames = "slot_id"))
public class Booking {

    public static final String STATUS_CONFIRMED = "CONFIRMED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_CONFIRMED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Booking() {
    }

    public Booking(Long userId, Slot slot) {
        this.userId = userId;
        this.slot = slot;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Slot getSlot() { return slot; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
