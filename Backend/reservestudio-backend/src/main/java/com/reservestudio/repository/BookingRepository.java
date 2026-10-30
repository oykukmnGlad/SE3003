package com.reservestudio.repository;

import com.reservestudio.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByStudioIdAndSlotStart(Long studioId, LocalDateTime slotStart);

    List<Booking> findByUserIdOrderBySlotStartAsc(Long userId);

    @Query("select b.slotStart from Booking b "
            + "where b.studioId = :studioId and b.slotStart >= :from and b.slotStart < :to")
    List<LocalDateTime> findBookedSlotStarts(@Param("studioId") Long studioId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);
}
