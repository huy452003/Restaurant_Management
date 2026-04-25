package com.common.entities;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.common.enums.UserRole;
import com.common.enums.UserStatus;
import com.common.enums.Gender;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity implements UserDetails {
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "fullname", nullable = false)
    private String fullname;
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "phone", nullable = false, unique = true)
    private String phone;
    @Column(name = "gender", nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Column(name = "birth", nullable = false)
    private LocalDate birth;
    @Column(name = "address", nullable = false)
    private String address;
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;
    @Column(name = "user_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    // version
    @Version
    @Column(name = "version")
    private Long version;
    
    // Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security convention: role phải có prefix "ROLE_"
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }
    @Override
    public String getUsername() {
        return this.username;
    }
    @Override
    public String getPassword() {
        return this.password;
    }

    // @Override
    // public boolean isEnabled() {
    //     return this.userStatus == UserStatus.ACTIVE;
    // }

    @OneToMany(mappedBy = "waiter", fetch = FetchType.LAZY)
    private List<OrderEntity> orders;
    @OneToMany(mappedBy = "cashier", fetch = FetchType.LAZY)
    private List<PaymentEntity> payments;
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
    private List<ShiftEntity> shifts;

    @PrePersist
    @PreUpdate
    private void normalizeFields() {
        if (this.email != null) {
            this.email = this.email.toLowerCase().trim();
        }
        if (this.username != null) {
            this.username = this.username.toLowerCase().trim();
        }
    }
}
