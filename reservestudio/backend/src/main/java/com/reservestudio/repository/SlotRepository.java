package com.reservestudio.repository;

import com.reservestudio.model.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            LocalDateTime from, LocalDateTime to);

    /**
     * Atomik "slotu kap": yalnızca slot hâlâ müsaitse 1 satır günceller.
     * İki istek aynı anda gelirse satır kilidi yüzünden biri bekler, diğeri commit
     * ettikten sonra koşulu tekrar değerlendirir ve 0 döner (yani kaybeder).
     * Çağıran, 0 dönerse "slot dolu" (409) demelidir. Transaction içinde çağrılmalı.
     */
    @Modifying
    @Query("update Slot s set s.available = false where s.id = :id and s.available = true")
    int markUnavailable(@Param("id") Long id);

    /** İptalde slotu tekrar müsait yapar. Transaction içinde çağrılmalı. */
    @Modifying
    @Query("update Slot s set s.available = true where s.id = :id")
    int markAvailable(@Param("id") Long id);
}
