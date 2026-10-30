package com.reservestudio.service;

import com.reservestudio.dto.Dtos.BookingResponse;
import com.reservestudio.exception.ApiException;
import com.reservestudio.model.Booking;
import com.reservestudio.repository.BookingRepository;
import com.reservestudio.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SlotService slotService;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          SlotService slotService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.slotService = slotService;
    }

    /**
     * İki katmanlı çakışma savunması:
     * 1) Hızlı ön kontrol (existsBy...) — güzel bir hata mesajı için.
     * 2) Asıl güvence: (studio_id, slot_start) UNIQUE kısıtı. Aynı anda gelen iki istek
     *    ön kontrolü birlikte geçse bile veritabanı yalnızca birini kabul eder,
     *    diğeri DataIntegrityViolationException ile 409'a çevrilir.
     *
     * Bu metot bilerek @Transactional DEĞİL: saveAndFlush kendi transaction'ında
     * çalışır ve kısıt ihlali burada yakalanabilir.
     */
    public BookingResponse create(Long userId, LocalDateTime slotStart) {
        if (!slotService.isValidSlotStart(slotStart)) {
            throw ApiException.badRequest("Slot tam saat olmalı ve çalışma saatleri içinde olmalı ("
                    + slotService.getOpenHour() + ":00 - " + slotService.getCloseHour() + ":00)");
        }
        if (!slotStart.isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Geçmiş bir slot için rezervasyon yapılamaz");
        }
        if (!userRepository.existsById(userId)) {
            throw ApiException.unauthorized("Oturum geçersiz");
        }
        if (bookingRepository.existsByStudioIdAndSlotStart(Booking.MAIN_STUDIO_ID, slotStart)) {
            throw slotTaken();
        }

        try {
            Booking saved = bookingRepository.saveAndFlush(new Booking(userId, slotStart));
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw slotTaken();
        }
    }

    public List<BookingResponse> findByUser(Long userId) {
        return bookingRepository.findByUserIdOrderBySlotStartAsc(userId).stream()
                .map(BookingService::toResponse)
                .toList();
    }

    public void cancel(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Rezervasyon bulunamadı"));

        if (!booking.getUserId().equals(userId)) {
            throw ApiException.forbidden("Bu rezervasyon size ait değil");
        }
        if (!booking.getSlotStart().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Geçmiş rezervasyon iptal edilemez");
        }
        // Satır silinir; böylece slot UNIQUE kısıtını tutmaz ve tekrar alınabilir hale gelir.
        bookingRepository.delete(booking);
    }

    private static ApiException slotTaken() {
        return ApiException.conflict("Bu slot zaten rezerve edilmiş");
    }

    private static BookingResponse toResponse(Booking b) {
        return new BookingResponse(b.getId(), b.getSlotStart(), b.getSlotStart().plusHours(1), b.getCreatedAt());
    }
}
