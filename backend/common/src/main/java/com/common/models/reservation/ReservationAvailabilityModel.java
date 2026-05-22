package com.common.models.reservation;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationAvailabilityModel {
    private Integer tableNumber;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate date;
    private List<String> bookedTimes;
}
