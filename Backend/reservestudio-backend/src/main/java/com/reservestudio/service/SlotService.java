package com.reservestudio.service;

import com.reservestudio.dto.Dtos.SlotResponse;
import com.reservestudio.model.Booking;
import com.reservestudio.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SlotService {

    private final BookingRepository bookingRepository;
    private final int openHour;
    private final int closeHour;

    public SlotService(BookingRepository bookingRepository,
                       @Value("${studio.open-hour:9}") int openHour,
                       @Value("${studio.close-hour:21}") int closeHour) {
        this.bookingRepository = bookingRepository;
        this.openHour = openHour;
        this.closeHour = closeHour;
    }

    /** Verilen gün için tüm saatlik slotları ve doluluk bilgisini döner. */
    public List<SlotResponse> getSlots(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        Set<LocalDateTime> booked = new HashSet<>(
                bookingRepository.findBookedSlotStarts(Booking.MAIN_STUDIO_ID, dayStart, dayStart.plusDays(1)));
        LocalDateTime now = LocalDateTime.now();

        List<SlotResponse> slots = new ArrayList<>();
        for (int hour = openHour; hour < closeHour; hour++) {
            LocalDateTime start = date.atTime(hour, 0);
            boolean available = !booked.contains(start) && start.isAfter(now);
            slots.add(new SlotResponse(start, start.plusHours(1), available));
        }
        return slots;
    }

    /** Slot başlangıcı geçerli mi (tam saat, çalışma saatleri içinde)? */
    public boolean isValidSlotStart(LocalDateTime slotStart) {
        boolean onTheHour = slotStart.getMinute() == 0 && slotStart.getSecond() == 0 && slotStart.getNano() == 0;
        int hour = slotStart.getHour();
        return onTheHour && hour >= openHour && hour < closeHour;
    }

    public int getOpenHour() {
        return openHour;
    }

    public int getCloseHour() {
        return closeHour;
    }
}
