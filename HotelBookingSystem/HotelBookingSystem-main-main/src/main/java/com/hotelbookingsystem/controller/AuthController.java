package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.Admin;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String doLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes ra) {

        // ===== STEP 1: Check if admin =====
        Admin admin = authService.adminLogin(email, password);
        if (admin != null) {
            session.setAttribute("admin", admin);
            return "redirect:/admin/dashboard";
        }

        // ===== STEP 2: Check if normal user =====
        User user = authService.login(email, password);
        if (user == null) {
            ra.addFlashAttribute("error", "Sai email hoặc mật khẩu");
            return "redirect:/login";
        }

        session.setAttribute("user", user);
        return "redirect:/rooms";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String confirmPassword,
            HttpSession session,
            RedirectAttributes ra) {

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            ra.addFlashAttribute("error", "Vui lòng nhập đầy đủ email và mật khẩu");
            return "redirect:/register";
        }

        if (confirmPassword != null && !password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/register";
        }

        User newUser = authService.register(email, password);
        if (newUser == null) {
            ra.addFlashAttribute("error", "Email đã được sử dụng");
            return "redirect:/register";
        }

        session.setAttribute("user", newUser);
        return "redirect:/rooms";
    }
}