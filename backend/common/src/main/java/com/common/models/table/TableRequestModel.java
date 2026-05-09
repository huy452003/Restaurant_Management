package com.common.models.table;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableRequestModel {
    @NotNull(message = "validate.table.tableNumber.required")
    @Min(value = 1, message = "validate.table.tableNumber.min")
    private Integer tableNumber;

    @Min(value = 1, message = "validate.table.capacity.min")
    private Integer capacity;

    @NotBlank(message = "validate.table.location.required")
    private String location;
}
