package com.common.models.reservation;

import java.time.LocalDateTime;

import com.common.enums.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationAdminRequestModel implements ReservationTablePayload{
    @NotBlank(message = "validate.reservation.customerName.required")
    @Size(min = 1, max = 50, message = "validate.reservation.customerName.size")
    private String customerName;
    @NotBlank(message = "validate.reservation.customerPhone.required")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "validate.reservation.customerPhone.invalidFormat")
    private String customerPhone;
    @NotBlank(message = "validate.reservation.customerEmail.required")
    @Email(message = "validate.reservation.customerEmail.invalidFormat")
    private String customerEmail;
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
    @NotNull(message = "validate.reservation.reservationStatus.required")
    @Enumerated(EnumType.STRING)
    private ReservationStatus reservationStatus;
    @Size(max = 300, message = "validate.reservation.specialRequest.size")
    private String specialRequest;
}
