package com.common.entities;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.common.enums.ShiftStatus;

@Entity
@Table(name = "shifts")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor 
public class ShiftEntity extends BaseEntity {
    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    @Column(name = "shift_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ShiftStatus shiftStatus;
    @Column(name = "notes")
    private String notes;

    // version
    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private UserEntity employee;
}
