package com.reservestudio.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Tek stüdyo için saatlik rezervasyon.
 * (studio_id, slot_start) üzerindeki UNIQUE kısıt, aynı slotun iki kez alınmasını
 * veritabanı seviyesinde engeller (race condition savunması).
 */
@Entity
@Table(name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_studio_slot",
                columnNames = {"studio_id", "slot_start"}))
public class Booking {

    public static final long MAIN_STUDIO_ID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId = MAIN_STUDIO_ID;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Booking() {
    }

    public Booking(Long userId, LocalDateTime slotStart) {
        this.userId = userId;
        this.slotStart = slotStart;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getStudioId() { return studioId; }
    public Long getUserId() { return userId; }
    public LocalDateTime getSlotStart() { return slotStart; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
