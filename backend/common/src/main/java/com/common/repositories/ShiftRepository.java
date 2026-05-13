package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.common.entities.ShiftEntity;
import com.common.enums.ShiftStatus;
import java.time.LocalDateTime;

@Repository
public interface ShiftRepository extends JpaRepository<ShiftEntity, Integer>, JpaSpecificationExecutor<ShiftEntity> {

    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM ShiftEntity s
        WHERE s.employee.id = :employeeId
          AND s.shiftStatus <> :excludedStatus
          AND (:excludeId IS NULL OR s.id <> :excludeId)
          AND s.endTime > :startTime
          AND :endTime > s.startTime
    """)
    boolean existsOverlappingShift(
        @Param("employeeId") Integer employeeId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("excludeId") Integer excludeId,
        @Param("excludedStatus") ShiftStatus excludedStatus
    );
}
