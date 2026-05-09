package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.common.entities.UserEntity;
import com.common.enums.UserRole;
import com.common.enums.UserStatus;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer>, JpaSpecificationExecutor<UserEntity> {
    // Tìm user theo email
    Optional<UserEntity> findByEmail(String email);
    List<UserEntity> findByEmailIn(List<String> emails);
    Optional<UserEntity> findByPhone(String phone);
    Optional<UserEntity> findByUsername(String username);
    
    // Tìm user theo role
    List<UserEntity> findByRole(UserRole role);
    List<UserEntity> findByUserStatus(UserStatus userStatus);

    // Kiểm tra xem user có tồn tại không
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);
    boolean existsByRole(UserRole role);
}
