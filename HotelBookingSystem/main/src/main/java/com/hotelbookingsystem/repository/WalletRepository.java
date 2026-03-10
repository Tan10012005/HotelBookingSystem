package com.hotelbookingsystem.repository;

import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /** Tìm ví theo user object */
    Optional<Wallet> findByUser(User user);

    /** Tìm ví theo userId (dùng khi chỉ có ID) */
    Optional<Wallet> findByUserId(Long userId);
}