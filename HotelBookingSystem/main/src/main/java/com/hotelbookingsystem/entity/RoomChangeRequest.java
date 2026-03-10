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

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "old_room_id", nullable = false)
    private Room oldRoom;

    @ManyToOne
    @JoinColumn(name = "new_room_id", nullable = false)
    private Room newRoom;

    @Column(nullable = false)
    private BigDecimal priceDifference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomChangeStatus status = RoomChangeStatus.PENDING;

    @Column(columnDefinition = "NVARCHAR(MAX)")
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