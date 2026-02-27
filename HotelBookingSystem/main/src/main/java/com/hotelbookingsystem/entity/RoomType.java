package com.hotelbookingsystem.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "room_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @OneToMany(mappedBy = "roomType", fetch = FetchType.LAZY)
    private List<Room> rooms;
}

