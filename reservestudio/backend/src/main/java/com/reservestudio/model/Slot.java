package com.reservestudio.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Tablo: slots (id, start_time, end_time, is_available, created_at) — saatlik stüdyo dilimi. */
@Entity
@Table(name = "slots")
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /** DB'de nullable olduğu için Boolean; null "müsait" sayılır. */
    @Column(name = "is_available")
    private Boolean available = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Slot() {
    }

    public Slot(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (available == null) {
            available = Boolean.TRUE;
        }
    }

    public Long getId() { return id; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public boolean isAvailable() { return !Boolean.FALSE.equals(available); }
}
