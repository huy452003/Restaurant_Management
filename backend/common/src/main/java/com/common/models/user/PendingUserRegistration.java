package com.common.models.user;

import java.time.LocalDate;

import com.common.configurations.FlexibleLocalDateDeserializer;
import com.common.enums.Gender;
import com.common.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingUserRegistration {
    private String registrationId;
    private String username;
    private String encodedPassword;
    private String fullname;
    private String email;
    private String phone;
    private Gender gender;
    @JsonFormat(pattern = "dd-MM-yyyy")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate birth;
    private String address;
    private UserRole role;
}
