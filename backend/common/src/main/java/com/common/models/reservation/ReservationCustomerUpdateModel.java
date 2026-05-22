package com.common.models.reservation;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationCustomerUpdateModel {
    @NotNull(message = "validate.reservation.reservationDate.required")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate reservationDate;
    @NotNull(message = "validate.reservation.reservationTime.required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime reservationTime;
    @NotNull(message = "validate.reservation.numberOfGuests.required")
    @Min(value = 1, message = "validate.reservation.numberOfGuests.min")
    private Integer numberOfGuests;
    @Size(max = 300, message = "validate.reservation.specialRequest.size")
    private String specialRequest;
}
