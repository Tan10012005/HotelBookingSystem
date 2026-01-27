package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.RoomStatus;
import com.hotelbookingsystem.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    private RoomRepository roomRepo;

    @Override
    public List<Room> getAvailableRooms() {
        return roomRepo.findByStatus(RoomStatus.AVAILABLE);
    }

    @Override
    public Room getRoomById(Long id) {
        return roomRepo.findById(id).orElse(null);
    }
}
