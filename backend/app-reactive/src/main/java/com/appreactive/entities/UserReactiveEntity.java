package com.appreactive.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

@Data
@Table("users")
public class UserReactiveEntity {
    @Id
    private Integer id;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    private String username;
    private String password;
    private String fullname;
    private String email;
    private String phone;
    private String gender;
    private LocalDate birth;
    private String address;
    private String role;

    @Column("user_status")
    private String userStatus;

    private Long version;
}
