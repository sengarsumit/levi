package com.koda.levi.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "name",nullable = false)
    private String name;

    @Column(name = "email",nullable = false, unique = true)
    private String email;

    @Column(name = "password",nullable = false)
    private String password;

    @Column(name = "role")
    private String role="ROLE_USER";

    @Column(name = "created_At")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
