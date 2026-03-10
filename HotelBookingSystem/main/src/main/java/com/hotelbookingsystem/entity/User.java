package com.hotelbookingsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
    private Boolean isActive = true;

    @Column(length = 20)
    private String authProvider;

    // ===================== WALLET =====================
    /** Số dư ví — được cộng khi admin xử lý hoàn tiền */
    @Column(name = "wallet_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal walletBalance = BigDecimal.ZERO;
    // ==================================================

    private LocalDateTime createdAt;

    public boolean isProfileComplete() {
        return fullName != null && !fullName.isBlank()
                && phoneNumber != null && !phoneNumber.isBlank()
                && citizenId != null && !citizenId.isBlank();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    /** Helper: lấy số dư ví an toàn (không null) */
    public BigDecimal safeWalletBalance() {
        return walletBalance != null ? walletBalance : BigDecimal.ZERO;
    }
}