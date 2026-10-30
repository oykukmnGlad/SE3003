package com.reservestudio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class Dtos {

    private Dtos() {
    }

    // ---------- İstekler ----------

    public record SignupRequest(
            @NotBlank(message = "Ad soyad zorunludur") @Size(max = 100) String fullName,
            @NotBlank(message = "E-posta zorunludur") @Email(message = "Geçerli bir e-posta girin") @Size(max = 150) String email,
            @NotBlank(message = "Şifre zorunludur") @Size(min = 8, max = 72, message = "Şifre 8-72 karakter olmalıdır") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "E-posta zorunludur") String email,
            @NotBlank(message = "Şifre zorunludur") String password) {
    }

    public record BookingRequest(
            @NotNull(message = "slotStart zorunludur") LocalDateTime slotStart) {
    }

    // ---------- Cevaplar ----------

    public record UserResponse(Long id, String fullName, String email) {
    }

    public record SlotResponse(LocalDateTime start, LocalDateTime end, boolean available) {
    }

    public record BookingResponse(Long id, LocalDateTime slotStart, LocalDateTime slotEnd, LocalDateTime createdAt) {
    }

    public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
    }
}
