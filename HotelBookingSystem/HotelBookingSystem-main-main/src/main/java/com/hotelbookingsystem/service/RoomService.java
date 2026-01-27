package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Room;

import java.util.List;

public interface RoomService {
    List<Room> getAvailableRooms();
    Room getRoomById(Long id);
}
