package com.common.models.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.common.enums.ShiftStatus;
import com.common.models.BaseModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShiftModel extends BaseModel{
    private Integer employeeId;
    private LocalDate shiftDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ShiftStatus shiftStatus;
    private String notes;
}
