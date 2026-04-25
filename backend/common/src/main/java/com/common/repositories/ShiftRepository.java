package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.common.entities.ShiftEntity;
import com.common.enums.ShiftStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<ShiftEntity, Integer>, JpaSpecificationExecutor<ShiftEntity> {
    // Tìm shifts theo employee
    List<ShiftEntity> findByEmployee_Id(Integer employeeId);
    
    // Tìm shifts theo status
    List<ShiftEntity> findByShiftStatus(ShiftStatus shiftStatus);
    
    // Tìm shifts theo ngày
    List<ShiftEntity> findByShiftDate(LocalDate date);
    
    // Tìm shifts của employee trong ngày
    List<ShiftEntity> findByEmployee_IdAndShiftDate(Integer employeeId, LocalDate date);
    
    // Tìm active shifts (đang diễn ra)
    @Query("SELECT s FROM ShiftEntity s WHERE s.shiftStatus = ShiftStatus.IN_PROGRESS " +
           "AND s.startTime <= :now AND s.endTime >= :now")
    List<ShiftEntity> findActiveShifts(@Param("now") LocalDateTime now);
    
    // Tìm shifts trong khoảng thời gian
    @Query("SELECT s FROM ShiftEntity s WHERE s.shiftDate BETWEEN :startDate AND :endDate")
    List<ShiftEntity> findShiftsBetweenDates(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    // Tìm shifts của employee trong khoảng thời gian
    @Query("SELECT s FROM ShiftEntity s WHERE s.employee.id = :employeeId " +
           "AND s.shiftDate BETWEEN :startDate AND :endDate")
    List<ShiftEntity> findEmployeeShiftsBetweenDates(
        @Param("employeeId") Integer employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
