package com.common.models.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;   

import jakarta.validation.constraints.NotBlank; 
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestModel {
    @NotBlank(message = "validate.user.username.required")
    @Size(min = 3, max = 50, message = "validate.user.username.size")
    private String username;
    
    @NotBlank(message = "validate.user.password.required")
    @Size(min = 6, max = 100, message = "validate.user.password.size")
    private String password;
}
