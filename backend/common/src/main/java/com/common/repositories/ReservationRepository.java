package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.common.entities.ReservationEntity;
import com.common.enums.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Integer>, JpaSpecificationExecutor<ReservationEntity> {
    // Tìm reservations theo table
    List<ReservationEntity> findByTable_TableNumber(Integer tableNumber);
    
    // Tìm reservations theo status
    List<ReservationEntity> findByReservationStatus(ReservationStatus reservationStatus);
    
    // Tìm reservations theo timestamp range
    List<ReservationEntity> findByReservationTsBetween(LocalDateTime start, LocalDateTime end);
    
    // Tìm reservations theo table và timestamp range (quan trọng để check conflict)
    List<ReservationEntity> findByTable_TableNumberAndReservationTsBetween(Integer tableNumber, LocalDateTime start, LocalDateTime end);
    
    // Tìm reservations theo customer phone
    List<ReservationEntity> findByCustomerPhone(String phone);
    
    // Tìm reservations theo customer email
    List<ReservationEntity> findByCustomerEmail(String email);
    
    // Tìm reservations trong khoảng thời gian
    @Query("SELECT r FROM ReservationEntity r WHERE CAST(r.reservationTs AS date) = :date " +
           "AND r.table.tableNumber = :tableNumber " +
           "AND r.reservationStatus <> :cancelledStatus")
    List<ReservationEntity> findActiveReservationsByTableAndDate(
        @Param("date") LocalDate date,
        @Param("tableNumber") Integer tableNumber,
        @Param("cancelledStatus") ReservationStatus cancelledStatus
    );

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
