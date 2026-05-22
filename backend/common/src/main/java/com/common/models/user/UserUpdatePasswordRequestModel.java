package com.common.models.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdatePasswordRequestModel {
    @NotBlank(message = "validate.user.oldPassword.required")
    @Size(min = 6, max = 100, message = "validate.user.oldPassword.size")
    private String oldPassword;

    @NotBlank(message = "validate.user.newPassword.required")
    @Size(min = 6, max = 100, message = "validate.user.newPassword.size")
    private String newPassword;

    @NotBlank(message = "validate.user.confirmNewPassword.required")
    @Size(min = 6, max = 100, message = "validate.user.confirmNewPassword.size")
    private String confirmNewPassword;
}
