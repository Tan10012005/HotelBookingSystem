package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String viewProfile(HttpSession session, Model model, RedirectAttributes ra) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        // Refresh user data from database
        User currentUser = userService.getUserById(user.getId());
        session.setAttribute("user", currentUser);

        model.addAttribute("user", currentUser);
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            @RequestParam String fullName,
            @RequestParam String phoneNumber,
            @RequestParam String citizenId,
            HttpSession session,
            RedirectAttributes ra) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        // Validation
        if (fullName == null || fullName.isBlank()) {
            ra.addFlashAttribute("error", "Vui lòng nhập họ tên");
            return "redirect:/profile";
        }

        if (phoneNumber == null || !phoneNumber.matches("^0[0-9]{9,10}$")) {
            ra.addFlashAttribute("error", "Số điện thoại không hợp lệ (phải bắt đầu bằng 0 và có 10-11 số)");
            return "redirect:/profile";
        }

        if (citizenId == null || !citizenId.matches("^[0-9]{9,12}$")) {
            ra.addFlashAttribute("error", "CCCD không hợp lệ (phải có 9-12 chữ số)");
            return "redirect:/profile";
        }

        // Update user
        User updatedUser = userService.updateProfile(user.getId(), fullName, phoneNumber, citizenId);
        session.setAttribute("user", updatedUser);

        ra.addFlashAttribute("success", "Cập nhật hồ sơ thành công!");
        return "redirect:/profile";
    }
}