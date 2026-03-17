package com.common.models.user;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.common.enums.Gender;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserNormalModel {
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
}
