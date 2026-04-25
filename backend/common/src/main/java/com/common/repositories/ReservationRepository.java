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
    List<ReservationEntity> findByTableId(Integer tableId);
    
    // Tìm reservations theo status
    List<ReservationEntity> findByReservationStatus(ReservationStatus reservationStatus);
    
    // Tìm reservations theo timestamp range
    List<ReservationEntity> findByReservationTsBetween(LocalDateTime start, LocalDateTime end);
    
    // Tìm reservations theo table và timestamp range (quan trọng để check conflict)
    List<ReservationEntity> findByTableIdAndReservationTsBetween(Integer tableId, LocalDateTime start, LocalDateTime end);
    
    // Tìm reservations theo customer phone
    List<ReservationEntity> findByCustomerPhone(String phone);
    
    // Tìm reservations theo customer email
    List<ReservationEntity> findByCustomerEmail(String email);
    
    // Tìm reservations trong khoảng thời gian
    @Query("SELECT r FROM ReservationEntity r WHERE FUNCTION('DATE', r.reservationTs) = :date " +
           "AND r.tableId = :tableId " +
           "AND r.reservationStatus != :cancelledStatus")
    List<ReservationEntity> findActiveReservationsByTableAndDate(
        @Param("date") LocalDate date,
        @Param("tableId") Integer tableId,
        @Param("cancelledStatus") ReservationStatus cancelledStatus
    );
}
