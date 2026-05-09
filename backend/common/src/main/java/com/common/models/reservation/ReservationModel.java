package com.common.models.reservation;

import com.common.enums.ReservationStatus;
import com.common.models.BaseModel;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationModel extends BaseModel implements ReservationTablePayload{
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private Integer tableNumber;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime reservationTs;
    private Integer numberOfGuests;
    private ReservationStatus reservationStatus;
    private String specialRequest;
}
