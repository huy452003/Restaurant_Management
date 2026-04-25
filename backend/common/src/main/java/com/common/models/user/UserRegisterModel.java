package com.common.models.user;

import java.time.LocalDate;

import com.common.enums.Gender;
import com.common.enums.UserRole;
import com.common.enums.UserStatus;
import com.common.models.BaseModel;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterModel extends BaseModel{
    private String username;
    private String fullname;
    private String email;
    private String phone;
    private Gender gender;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate birth;
    private Integer age;
    private String address;
    private UserRole role;
    private UserStatus userStatus;
    private String accessToken;
    private String expires;
    private String refreshToken;
    private String refreshExpires;
    private String verificationToken;
    private String verificationTokenExpires;
}
