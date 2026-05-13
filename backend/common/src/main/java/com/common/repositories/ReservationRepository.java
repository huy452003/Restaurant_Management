package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.common.entities.ReservationEntity;
import com.common.enums.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Integer>, JpaSpecificationExecutor<ReservationEntity> {

    @Query("""
        SELECT COUNT(r) > 0
        FROM ReservationEntity r
        WHERE r.table.tableNumber = :tableNumber
          AND r.reservationStatus IN :activeStatuses
          AND r.reservationTs BETWEEN :startTs AND :endTs
          AND (:excludeId IS NULL OR r.id <> :excludeId)
    """)
    boolean existsActiveTimeslotConflict(
        @Param("tableNumber") Integer tableNumber,
        @Param("startTs") LocalDateTime startTs,
        @Param("endTs") LocalDateTime endTs,
        @Param("activeStatuses") List<ReservationStatus> activeStatuses,
        @Param("excludeId") Integer excludeId
    );

    @Query("""
        SELECT COUNT(r) > 0
        FROM ReservationEntity r
        WHERE r.table.tableNumber = :tableNumber
          AND r.reservationStatus IN :activeStatuses
          AND r.reservationTs BETWEEN :windowStart AND :windowEnd
          AND (:excludeId IS NULL OR r.id <> :excludeId)
    """)
    boolean existsActiveReservationInWindow(
        @Param("tableNumber") Integer tableNumber,
        @Param("windowStart") LocalDateTime windowStart,
        @Param("windowEnd") LocalDateTime windowEnd,
        @Param("activeStatuses") List<ReservationStatus> activeStatuses,
        @Param("excludeId") Integer excludeId
    );
}
