package com.reservestudio.service;

import com.reservestudio.dto.Dtos.SlotResponse;
import com.reservestudio.exception.ApiException;
import com.reservestudio.model.Slot;
import com.reservestudio.repository.BookingRepository;
import com.reservestudio.repository.SlotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * slots tablosu ön-doldurulmuş gelmez; bu servis saatlik slotları ihtiyaç oldukça üretir
 * (açılışta bugünden itibaren N gün, ayrıca bir güne bakıldığında o günü tamamlar).
 */
@Service
public class SlotService {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final TransactionTemplate tx;
    private final int openHour;
    private final int closeHour;
    private final int windowDays;

    public SlotService(SlotRepository slotRepository,
                       BookingRepository bookingRepository,
                       PlatformTransactionManager transactionManager,
                       @Value("${studio.open-hour:9}") int openHour,
                       @Value("${studio.close-hour:21}") int closeHour,
                       @Value("${studio.booking-window-days:30}") int windowDays) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.tx = new TransactionTemplate(transactionManager);
        this.openHour = openHour;
        this.closeHour = closeHour;
        this.windowDays = windowDays;
    }

    /**
     * Bir günün eksik saatlik slotlarını üretir. synchronized + kendi transaction'ı:
     * aynı JVM'de iki istek aynı günü aynı anda üretip çift slot oluşturamaz.
     * (Birden fazla sunucu çalıştırılacaksa slots.start_time üzerine UNIQUE eklenmeli:
     * bkz. db/suggested-additions.sql)
     */
    public synchronized void ensureSlotsForDate(LocalDate date) {
        tx.executeWithoutResult(status -> {
            LocalDateTime dayStart = date.atStartOfDay();
            Set<LocalDateTime> existing = slotRepository
                    .findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                            dayStart, dayStart.plusDays(1))
                    .stream().map(Slot::getStartTime).collect(Collectors.toSet());

            List<Slot> missing = new ArrayList<>();
            for (int hour = openHour; hour < closeHour; hour++) {
                LocalDateTime start = date.atTime(hour, 0);
                if (!existing.contains(start)) {
                    missing.add(new Slot(start, start.plusHours(1)));
                }
            }
            if (!missing.isEmpty()) {
                slotRepository.saveAll(missing);
            }
        });
    }

    /** Bugünden rezervasyon penceresinin sonuna kadar tüm slotları hazırlar. */
    public void ensureWindow() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i <= windowDays; i++) {
            ensureSlotsForDate(today.plusDays(i));
        }
    }

    /** Verilen gün için tüm saatlik slotlar ve doluluk bilgisi. */
    public List<SlotResponse> getSlots(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today.plusDays(windowDays))) {
            throw ApiException.badRequest("En fazla " + windowDays + " gün ilerisi görüntülenebilir");
        }
        if (!date.isBefore(today)) {
            ensureSlotsForDate(date);
        }

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        List<Slot> slots = slotRepository
                .findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(from, to);
        Set<Long> booked = new HashSet<>(bookingRepository.findBookedSlotIds(from, to));
        LocalDateTime now = LocalDateTime.now();

        return slots.stream()
                .map(s -> new SlotResponse(
                        s.getId(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.isAvailable() && !booked.contains(s.getId()) && s.getStartTime().isAfter(now)))
                .toList();
    }
}
