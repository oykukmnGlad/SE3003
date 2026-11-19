package com.reservestudio.repository;

import com.reservestudio.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("select b from Booking b where b.userId = :userId order by b.slot.startTime asc")
    List<Booking> findByUser(@Param("userId") Long userId);

    @Query("select b.slot.id from Booking b "
            + "where b.slot.startTime >= :from and b.slot.startTime < :to")
    List<Long> findBookedSlotIds(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(b) from Booking b where b.slot.id = :slotId")
    long countBySlotId(@Param("slotId") Long slotId);
}
