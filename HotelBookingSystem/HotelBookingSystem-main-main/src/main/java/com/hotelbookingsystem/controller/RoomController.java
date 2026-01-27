package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping
    public String listRooms(Model model) {
        List<Room> rooms = roomService.getAvailableRooms();
        model.addAttribute("rooms", rooms);
        return "roomList"; // ✅ KHỚP FILE
    }

    @GetMapping("/{id}")
    public String selectRoom(@PathVariable Long id, Model model) {
        Room room = roomService.getRoomById(id);
        model.addAttribute("room", room);
        return "bookingForm"; // ✅ KHỚP FILE
    }
}
