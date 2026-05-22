package com.common.models.reservation;

import com.common.enums.ReservationStatus;
import com.common.models.BaseModel;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationModel extends BaseModel {
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private Integer tableNumber;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate reservationDate;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime reservationTime;
    private Integer numberOfGuests;
    private ReservationStatus reservationStatus;
    private String specialRequest;
}
