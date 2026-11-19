package com.reservestudio.service;

import com.reservestudio.dto.Dtos.BookingResponse;
import com.reservestudio.exception.ApiException;
import com.reservestudio.model.Booking;
import com.reservestudio.model.Slot;
import com.reservestudio.repository.BookingRepository;
import com.reservestudio.repository.SlotRepository;
import com.reservestudio.repository.UserRepository;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate tx;

    public BookingService(BookingRepository bookingRepository,
                          SlotRepository slotRepository,
                          UserRepository userRepository,
                          PlatformTransactionManager transactionManager) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /**
     * Çift katmanlı çakışma savunması (R-01 / WP-03):
     * 1) Atomik "slotu kap" UPDATE'i (slots.is_available true -> false). Aynı anda gelen
     *    iki istekten yalnızca biri 1 satır günceller, diğeri 0 görür ve 409 alır.
     * 2) Asıl güvence: bookings.slot_id UNIQUE kısıtı. 1. adım bir şekilde aşılsa bile
     *    veritabanı ikinci kaydı reddeder; tüm transaction geri alınır ve 409'a çevrilir.
     *
     * Transaction'ı TransactionTemplate ile elle yönetiyoruz ki kısıt ihlalini
     * transaction'ın DIŞINDA yakalayıp anlamlı bir hataya çevirebilelim.
     */
    public BookingResponse create(Long userId, Long slotId) {
        try {
            return tx.execute(status -> doCreate(userId, slotId));
        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
            throw slotTaken();
        }
    }

    private BookingResponse doCreate(Long userId, Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> ApiException.notFound("Slot bulunamadı"));

        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Geçmiş bir slot için rezervasyon yapılamaz");
        }
        if (!userRepository.existsById(userId)) {
            throw ApiException.unauthorized("Oturum geçersiz");
        }
        if (slotRepository.markUnavailable(slotId) == 0) {
            throw slotTaken(); // dolu ya da yönetici tarafından kapatılmış
        }

        Booking saved = bookingRepository.saveAndFlush(new Booking(userId, slot));
        return toResponse(saved);
    }

    public List<BookingResponse> findByUser(Long userId) {
        return bookingRepository.findByUser(userId).stream()
                .map(BookingService::toResponse)
                .toList();
    }

    /**
     * İptal: bookings satırı silinir ve slot tekrar müsait yapılır. (UNIQUE(slot_id) olduğu için
     * "CANCELLED" durumunda satırı tutarsak slot bir daha alınamazdı.)
     */
    public void cancel(Long userId, Long bookingId) {
        tx.executeWithoutResult(status -> {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> ApiException.notFound("Rezervasyon bulunamadı"));

            if (!booking.getUserId().equals(userId)) {
                throw ApiException.forbidden("Bu rezervasyon size ait değil");
            }
            if (!booking.getSlot().getStartTime().isAfter(LocalDateTime.now())) {
                throw ApiException.badRequest("Geçmiş rezervasyon iptal edilemez");
            }

            Long slotId = booking.getSlot().getId();
            bookingRepository.delete(booking);
            bookingRepository.flush();
            slotRepository.markAvailable(slotId);
        });
    }

    private static ApiException slotTaken() {
        return ApiException.conflict("Bu slot zaten rezerve edilmiş");
    }

    private static BookingResponse toResponse(Booking b) {
        Slot s = b.getSlot();
        return new BookingResponse(b.getId(), s.getId(), s.getStartTime(), s.getEndTime(),
                b.getStatus(), b.getCreatedAt());
    }
}
