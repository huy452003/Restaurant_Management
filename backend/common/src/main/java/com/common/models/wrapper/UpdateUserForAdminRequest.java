package com.common.models.wrapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.common.models.UpdateUserForAdminModel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserForAdminRequest {
    @NotNull
    @NotEmpty
    private List<Integer> userIds;

    @NotNull
    @NotEmpty
    @Valid
    private List<UpdateUserForAdminModel> updates;

}
