package com.common.models.wrapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WrapperUpdateRequest<T> {
    @NotNull
    @NotEmpty
    private List<Integer> ids;

    @NotNull
    @NotEmpty
    @Valid
    private List<T> updates;

}
