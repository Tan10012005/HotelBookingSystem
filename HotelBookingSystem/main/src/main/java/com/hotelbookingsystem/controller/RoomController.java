package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.RoomType;
import com.hotelbookingsystem.repository.RoomTypeRepository;
import com.hotelbookingsystem.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomTypeRepository roomTypeRepo;

    @GetMapping
    public String listRooms(Model model) {
        List<Room> rooms = roomService.getAvailableRooms();
        List<RoomType> roomTypes = roomTypeRepo.findAll();
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypes", roomTypes);
        return "roomList";
    }

    @GetMapping("/{id}")
    public String selectRoom(@PathVariable Long id, Model model) {
        Room room = roomService.getRoomById(id);
        model.addAttribute("room", room);
        return "bookingForm";
    }
}
