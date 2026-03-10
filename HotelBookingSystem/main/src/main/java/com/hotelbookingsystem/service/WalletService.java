package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.entity.Wallet;
import com.hotelbookingsystem.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    // =========================================================
    //  Lấy hoặc tạo mới ví cho user
    // =========================================================
    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUser(user).orElseGet(() -> {
            Wallet w = Wallet.builder()
                    .user(user)
                    .balance(BigDecimal.ZERO)
                    .build();
            return walletRepository.save(w);
        });
    }

    // =========================================================
    //  Lấy số dư ví — trả về ZERO nếu chưa có ví
    // =========================================================
    public BigDecimal getBalance(User user) {
        if (user == null || user.getId() == null) return BigDecimal.ZERO;
        return walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    // =========================================================
    //  Cộng tiền vào ví (khi admin xử lý hoàn tiền)
    // =========================================================
    @Transactional
    public void credit(User user, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        Wallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    // =========================================================
    //  Trừ tiền từ ví (khi user thanh toán booking bằng ví)
    //  @return true nếu đủ số dư và trừ thành công
    // =========================================================
    @Transactional
    public boolean debit(User user, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        Wallet wallet = getOrCreateWallet(user);
        if (wallet.getBalance().compareTo(amount) < 0) return false; // không đủ số dư
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        return true;
    }
}