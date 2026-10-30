package com.reservestudio;

import com.reservestudio.dto.Dtos.SignupRequest;
import com.reservestudio.exception.ApiException;
import com.reservestudio.repository.BookingRepository;
import com.reservestudio.service.AuthService;
import com.reservestudio.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * M1/M2 başarı ölçütü: "0 overlapping bookings during system testing".
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
    BookingRepository bookingRepository;

    @Test
    void onlyOneOfManySimultaneousBookingsSucceeds() throws Exception {
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            userIds.add(authService.signup(new SignupRequest(
                    "User " + i, "race-" + UUID.randomUUID() + "@test.com", "password123")).id());
        }

        LocalDateTime slot = LocalDate.now().plusDays(10).atTime(16, 0);
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
                    bookingService.create(userId, slot);
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
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, success.get(), "Tam olarak 1 rezervasyon başarılı olmalı");
        assertEquals(THREADS - 1, conflict.get(), "Diğerleri 409 Conflict almalı");
        assertEquals(0, other.get(), "Beklenmeyen hata olmamalı");
        assertEquals(1, bookingRepository.findBookedSlotStarts(1L, slot, slot.plusHours(1)).size(),
                "Veritabanında o slot için tek kayıt olmalı");
    }
}
