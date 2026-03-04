package com.hotelbookingsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String role; // "USER" hoặc "ADMIN"

    private String fullName;

    @Column(length = 15)
    private String phoneNumber;

    @Column(length = 20)
    private String citizenId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true; // true = active, false = banned

    @Column(length = 20)
    private String authProvider;  // "LOCAL" hoặc "GOOGLE"

    private LocalDateTime createdAt;

    public boolean isProfileComplete() {
        return fullName != null && !fullName.isBlank()
                && phoneNumber != null && !phoneNumber.isBlank()
                && citizenId != null && !citizenId.isBlank();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}