package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.Admin;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.repository.RoomRepository;
import com.hotelbookingsystem.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private BookingRepository bookingRepo;

    // ===== DASHBOARD =====
    @GetMapping({"/dashboard", ""})
    public String dashboard(HttpSession session, Model model, RedirectAttributes ra) {
        Admin admin = (Admin) session.getAttribute("admin");

        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập với tài khoản admin");
            return "redirect:/login";
        }

        // Statistics
        long totalUsers = userRepo.count();
        long totalRooms = roomRepo.count();
        long totalBookings = bookingRepo.count();

        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("totalBookings", totalBookings);

        return "admin/dashboard";
    }

    // ===== LOGOUT =====
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("admin");
        return "redirect:/login";
    }
}