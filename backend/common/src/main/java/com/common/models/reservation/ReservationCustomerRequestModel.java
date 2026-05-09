package com.common.models.reservation;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationCustomerRequestModel implements ReservationTablePayload {
    @NotNull(message = "validate.reservation.tableNumber.required")
    @Min(value = 1, message = "validate.reservation.tableNumber.min")
    private Integer tableNumber;
    @NotNull(message = "validate.reservation.reservationTs.required")
    @FutureOrPresent(message = "validate.reservation.reservationTs.mustNotBeInPast")
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime reservationTs;
    @NotNull(message = "validate.reservation.numberOfGuests.required")
    @Min(value = 1, message = "validate.reservation.numberOfGuests.min")
    private Integer numberOfGuests;
    @Size(max = 300, message = "validate.reservation.specialRequest.size")
    private String specialRequest;
}
