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
            @NotBlank(message = "Kullanıcı adı zorunludur")
            @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter olmalıdır") String username,
            @NotBlank(message = "E-posta zorunludur")
            @Email(message = "Geçerli bir e-posta girin")
            @Size(max = 100, message = "E-posta en fazla 100 karakter olabilir") String email,
            @NotBlank(message = "Şifre zorunludur")
            @Size(min = 8, max = 72, message = "Şifre 8-72 karakter olmalıdır") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "E-posta zorunludur") String email,
            @NotBlank(message = "Şifre zorunludur") String password) {
    }

    public record BookingRequest(
            @NotNull(message = "slotId zorunludur") Long slotId) {
    }

    // ---------- Cevaplar ----------

    public record UserResponse(Long id, String username, String email, String role) {
    }

    public record SlotResponse(Long id, LocalDateTime start, LocalDateTime end, boolean available) {
    }

    public record BookingResponse(Long id, Long slotId, LocalDateTime slotStart, LocalDateTime slotEnd,
                                  String status, LocalDateTime createdAt) {
    }

    public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
    }
}
