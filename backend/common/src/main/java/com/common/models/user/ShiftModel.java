package com.common.models.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.common.enums.ShiftStatus;
import com.common.models.BaseModel;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShiftModel extends BaseModel{
    @NotNull(message = "validate.shift.employeeId.required")
    @Min(value = 1, message = "validate.shift.employeeId.min")
    private Integer employeeId;
    @NotNull(message = "validate.shift.shiftDate.required")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate shiftDate;
    @NotNull(message = "validate.shift.startTime.required")
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime startTime;
    @NotNull(message = "validate.shift.endTime.required")
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime endTime;
    private Integer totalWorkingHours;
    @NotNull(message = "validate.shift.shiftStatus.required")
    @Enumerated(EnumType.STRING)
    private ShiftStatus shiftStatus;
    @Size(max = 300, message = "validate.shift.notes.size")
    private String notes;
}
