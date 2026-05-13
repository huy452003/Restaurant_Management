package com.common.models.order;

import java.time.LocalDateTime;

import com.common.enums.OrderStatus;
import com.common.enums.OrderType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAdminRequestModel {
    @NotBlank(message = "validate.reservation.customerName.required")
    @Size(min = 1, max = 50, message = "validate.reservation.customerName.size")
    private String customerName;
    @NotBlank(message = "validate.reservation.customerPhone.required")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "validate.reservation.customerPhone.invalidFormat")
    private String customerPhone;
    @NotBlank(message = "validate.reservation.customerEmail.required")
    @Email(message = "validate.reservation.customerEmail.invalidFormat")
    private String customerEmail;
    @NotNull(message = "validate.order.tableNumber.required")
    @Min(value = 1, message = "validate.order.tableNumber.min")
    private Integer tableNumber;
    @NotNull(message = "validate.order.waiterId.required")
    @Min(value = 1, message = "validate.order.waiterId.min")
    private Integer waiterId;
    @NotNull(message = "validate.order.orderStatus.required")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    @NotNull(message = "validate.order.orderType.required")
    @Enumerated(EnumType.STRING)
    private OrderType orderType;
    @Size(max = 300, message = "validate.order.notes.size")
    private String notes;
}
