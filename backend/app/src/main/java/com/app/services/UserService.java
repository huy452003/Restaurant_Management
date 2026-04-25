package com.app.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.models.user.LoginRequestModel;
import com.common.models.user.RegisterRequestModel;
import com.common.models.user.UpdateUserForAdminModel;
import com.common.models.user.UpdateUserNormalModel;
import com.common.models.user.UserModel;
import com.common.models.user.UserLoginModel;
import com.common.models.user.UserRegisterModel;
import com.common.enums.Gender;
import com.common.enums.UserRole;
import com.common.enums.UserStatus;

public interface UserService {
    Page<UserModel> filters(
        Integer id, String username, String fullname,
        String email, String phone, Gender gender,
        LocalDate birth, String address, UserRole role, UserStatus userStatus,
        Pageable pageable
    );
    List<UserRegisterModel> creates(List<RegisterRequestModel> registers);
    UserLoginModel login(LoginRequestModel req);
    void logout(String username);
    UserModel updateNormal(UpdateUserNormalModel update, Integer userId);
    List<UserModel> updatesForAdmin(List<UpdateUserForAdminModel> updates, List<Integer> userIds);
    UserModel verifyAndActivate(String verificationToken);
    String resendVerificationToken(Integer userId);
}
