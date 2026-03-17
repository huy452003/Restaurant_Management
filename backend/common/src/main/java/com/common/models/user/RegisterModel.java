package com.common.models.user;

import com.common.enums.Gender;
import com.common.enums.UserRole;
import com.common.models.BaseModel;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class RegisterModel extends BaseModel{
    @NotBlank(message = "validate.user.username.required")
    @Size(min = 3, max = 50, message = "validate.user.username.size")
    private String username;
    
    @NotBlank(message = "validate.user.password.required")
    @Size(min = 6, max = 100, message = "validate.user.password.size")
    private String password;
    
    @NotBlank(message = "validate.user.fullname.required")
    @Size(max = 100, message = "validate.user.fullname.size")
    private String fullname;
    
    @NotBlank(message = "validate.user.email.required")
    @Email(message = "validate.user.email.invalidFormat")
    private String email;
    
    @NotBlank(message = "validate.user.phone.required")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "validate.user.phone.invalidFormat")
    private String phone;
    
    @NotNull(message = "validate.user.gender.required")
    private Gender gender;
    
    @NotNull(message = "validate.user.birth.required")
    @PastOrPresent(message = "validate.user.birth.invalidFormat")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate birth;
    
    @NotBlank(message = "validate.user.address.required")
    @Size(max = 255, message = "validate.user.address.size")
    private String address;
    
    @NotNull(message = "validate.user.role.required")
    private UserRole role;
}
