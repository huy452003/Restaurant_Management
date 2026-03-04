package com.common.models.wrapper;

import java.util.List;

import com.common.models.UpdateUserNormalModel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserNormalRequest {
    @NotNull
    @NotEmpty
    private List<Integer> userIds;

    @NotNull
    @NotEmpty
    @Valid
    private List<UpdateUserNormalModel> updates;
}
