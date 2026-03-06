package com.hotelbookingsystem.entity;

import com.hotelbookingsystem.enums.RefundTransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Tên ngân hàng */
    @Column(name = "bank_name", nullable = false)
    private String bankName;

    /** Số tài khoản */
    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    /** Tên chủ tài khoản */
    @Column(name = "account_holder_name", nullable = false)
    private String accountHolderName;

    /** Số tiền hoàn */
    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    /** Phần trăm hoàn */
    @Column(name = "refund_percentage", nullable = false)
    private Integer refundPercentage;

    /** Trạng thái giao dịch hoàn tiền */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefundTransactionStatus status;

    /** Thời hạn phải hoàn tiền (24h sau khi hủy) */
    @Column(name = "refund_deadline")
    private LocalDateTime refundDeadline;

    /** Ghi chú của admin khi thực hiện hoàn tiền */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    /** Thời gian tạo */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Thời gian admin thực hiện hoàn tiền */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = RefundTransactionStatus.PENDING;
        // Deadline hoàn tiền: 24h sau khi hủy
        this.refundDeadline = LocalDateTime.now().plusHours(24);
    }
}