package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.common.entities.ReservationEntity;
import com.common.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Integer>, JpaSpecificationExecutor<ReservationEntity> {

    @Query("""
        SELECT COUNT(r) > 0
        FROM ReservationEntity r
        WHERE r.table.tableNumber = :tableNumber
          AND r.reservationDate = :reservationDate
          AND r.reservationTime = :reservationTime
          AND r.reservationStatus IN :activeStatuses
          AND (:excludeId IS NULL OR r.id <> :excludeId)
    """)
    boolean existsActiveSlot(
        @Param("tableNumber") Integer tableNumber,
        @Param("reservationDate") LocalDate reservationDate,
        @Param("reservationTime") LocalTime reservationTime,
        @Param("activeStatuses") List<ReservationStatus> activeStatuses,
        @Param("excludeId") Integer excludeId
    );

    @Query("""
        SELECT COUNT(r) > 0
        FROM ReservationEntity r
        WHERE r.table.tableNumber = :tableNumber
          AND r.reservationStatus IN :activeStatuses
          AND (:excludeId IS NULL OR r.id <> :excludeId)
    """)
    boolean existsActiveReservationOnTable(
        @Param("tableNumber") Integer tableNumber,
        @Param("activeStatuses") List<ReservationStatus> activeStatuses,
        @Param("excludeId") Integer excludeId
    );
}
