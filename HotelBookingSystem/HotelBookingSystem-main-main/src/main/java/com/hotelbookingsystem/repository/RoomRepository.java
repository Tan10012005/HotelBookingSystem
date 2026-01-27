package com.hotelbookingsystem.repository;

import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByStatus(RoomStatus status);
}