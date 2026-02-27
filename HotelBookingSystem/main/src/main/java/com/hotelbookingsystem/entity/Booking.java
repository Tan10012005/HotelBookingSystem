package com.hotelbookingsystem.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hotelbookingsystem.enums.BookingStatus;
import com.hotelbookingsystem.enums.CancellationReason;
import com.hotelbookingsystem.enums.CheckInStatus;
import com.hotelbookingsystem.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate checkIn;

    @Column(nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private Integer guests;

    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus refundStatus;

    @Column(name = "refund_percentage", columnDefinition = "INT DEFAULT 100")
    private Integer refundPercentage = 100;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason")
    private CancellationReason cancellationReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========== 🆕 THÊM CÁC TRƯỜNG CHO WORKFLOW 3 ==========

    /** Mã QR code check-in (dạng string Base64) */
    @Column(name = "qr_code", columnDefinition = "LONGTEXT")
    private String qrCode;

    /** Trạng thái check-in: PENDING (chưa check-in), CHECKED_IN (đã check-in), CHECKED_OUT */
    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_status", columnDefinition = "VARCHAR(50) DEFAULT 'PENDING'")
    private CheckInStatus checkInStatus = CheckInStatus.PENDING;

    /** Thời gian khách check-in thực tế */
    @Column(name = "actual_check_in_time")
    private LocalDateTime actualCheckInTime;

    /** Thời gian khách check-out thực tế */
    @Column(name = "actual_check_out_time")
    private LocalDateTime actualCheckOutTime;

    /** Lưu ý/ghi chú khi check-in */
    @Column(name = "check_in_notes", columnDefinition = "TEXT")
    private String checkInNotes;

    // ========== END NEW FIELDS ==========

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = BookingStatus.PENDING_CONFIRM;
        this.refundStatus = RefundStatus.NONE;
        this.refundPercentage = 100;
        this.checkInStatus = CheckInStatus.PENDING;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}