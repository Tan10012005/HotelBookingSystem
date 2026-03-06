package com.hotelbookingsystem.enums;

public enum RefundTransactionStatus {
    PENDING,        // Chờ admin xử lý
    PROCESSING,     // Admin đang xử lý
    COMPLETED,      // Đã hoàn tiền thành công
    FAILED,         // Hoàn tiền thất bại
    EXPIRED         // Quá hạn 24h chưa xử lý
}