package com.hotelbookingsystem.entity;

import com.hotelbookingsystem.enums.RoomChangeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Booking cần đổi phòng */
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** User yêu cầu đổi */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Phòng hiện tại */
    @ManyToOne
    @JoinColumn(name = "old_room_id", nullable = false)
    private Room oldRoom;

    /** Phòng muốn chuyển sang */
    @ManyToOne
    @JoinColumn(name = "new_room_id", nullable = false)
    private Room newRoom;

    /**
     * Chênh lệch giá = (newRoom.pricePerNight - oldRoom.pricePerNight) * số đêm
     * > 0: upgrade (user phải trả thêm)
     * = 0: ngang hạng (không phát sinh thêm)
     */
    @Column(nullable = false)
    private BigDecimal priceDifference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomChangeStatus status = RoomChangeStatus.PENDING;

    /** Ghi chú của admin khi duyệt/từ chối */
    @Column(columnDefinition = "TEXT")
    private String adminNote;

    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    @PrePersist
    public void onCreate() {
        this.requestedAt = LocalDateTime.now();
        this.status = RoomChangeStatus.PENDING;
    }

    @PreUpdate
    public void onUpdate() {
        this.processedAt = LocalDateTime.now();
    }
}