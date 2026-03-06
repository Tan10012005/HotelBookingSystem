package com.hotelbookingsystem.entity;

import com.hotelbookingsystem.enums.BookingStatus;
import com.hotelbookingsystem.enums.CancellationReason;
import com.hotelbookingsystem.enums.CheckInStatus;
import com.hotelbookingsystem.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    // ========== WORKFLOW 3 FIELDS ==========

    @Column(name = "qr_code", columnDefinition = "LONGTEXT")
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_status", columnDefinition = "VARCHAR(50) DEFAULT 'PENDING'")
    private CheckInStatus checkInStatus = CheckInStatus.PENDING;

    @Column(name = "actual_check_in_time")
    private LocalDateTime actualCheckInTime;

    @Column(name = "actual_check_out_time")
    private LocalDateTime actualCheckOutTime;

    @Column(name = "check_in_notes", columnDefinition = "TEXT")
    private String checkInNotes;

    // ========== REFUND TRANSACTION LINK ==========

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RefundTransaction refundTransaction;

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