package com.app.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.models.user.LoginModel;
import com.common.models.user.RegisterModel;
import com.common.models.user.UpdateUserForAdminModel;
import com.common.models.user.UpdateUserNormalModel;
import com.common.models.user.UserModel;
import com.common.models.user.UserSecurityModel;
import com.common.enums.Gender;
import com.common.enums.UserRole;
import com.common.enums.UserStatus;

public interface UserService {
    List<UserModel> getAll();
    List<UserSecurityModel> creates(List<RegisterModel> registers);
    UserSecurityModel login(LoginModel req);
    void logout(String username);
    UserModel updateNormal(UpdateUserNormalModel update, Integer userId);
    List<UserModel> updatesForAdmin(List<UpdateUserForAdminModel> updates, List<Integer> userIds);
    Page<UserModel> filters(
        Integer id, String username, String fullname,
        String email, String phone, Gender gender,
        LocalDate birth, String address, UserRole role, UserStatus userStatus,
        Pageable pageable
    );
    UserModel verifyAndActivate(String verificationToken);
}
