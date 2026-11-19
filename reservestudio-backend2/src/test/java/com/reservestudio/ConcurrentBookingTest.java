package com.reservestudio;

import com.reservestudio.dto.Dtos.SignupRequest;
import com.reservestudio.dto.Dtos.SlotResponse;
import com.reservestudio.exception.ApiException;
import com.reservestudio.repository.BookingRepository;
import com.reservestudio.repository.SlotRepository;
import com.reservestudio.service.AuthService;
import com.reservestudio.service.BookingService;
import com.reservestudio.service.SlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * M2 başarı ölçütü: "0 overlapping bookings during system testing" (risk R-01 / WP-03).
 * Aynı slota aynı anda birçok kullanıcı istek atar; yalnızca biri başarılı olmalıdır.
 */
@SpringBootTest
class ConcurrentBookingTest {

    private static final int THREADS = 20;

    @Autowired
    AuthService authService;
    @Autowired
    BookingService bookingService;
    @Autowired
    SlotService slotService;
    @Autowired
    SlotRepository slotRepository;
    @Autowired
    BookingRepository bookingRepository;

    @Test
    void onlyOneOfManySimultaneousBookingsSucceeds() throws Exception {
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            String s = UUID.randomUUID().toString().substring(0, 8);
            userIds.add(authService.signup(new SignupRequest("race" + s, "race-" + s + "@test.com", "password123")).id());
        }

        LocalDate date = LocalDate.now().plusDays(12);
        SlotResponse target = slotService.getSlots(date).stream()
                .filter(s -> s.start().getHour() == 16)
                .findFirst().orElseThrow();
        long slotId = target.id();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (Long userId : userIds) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    bookingService.create(userId, slotId);
                    success.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getStatus() == HttpStatus.CONFLICT) {
                        conflict.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                    }
                } catch (Exception e) {
                    other.incrementAndGet();
                }
            }));
        }

        ready.await();
        go.countDown(); // hepsi aynı anda başlasın
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, success.get(), "Tam olarak 1 rezervasyon başarılı olmalı");
        assertEquals(THREADS - 1, conflict.get(), "Diğerleri 409 Conflict almalı");
        assertEquals(0, other.get(), "Beklenmeyen hata olmamalı");
        assertEquals(1, bookingRepository.countBySlotId(slotId), "O slot için tek rezervasyon satırı olmalı");
        assertFalse(slotRepository.findById(slotId).orElseThrow().isAvailable(), "Slot dolu işaretlenmiş olmalı");
    }
}
