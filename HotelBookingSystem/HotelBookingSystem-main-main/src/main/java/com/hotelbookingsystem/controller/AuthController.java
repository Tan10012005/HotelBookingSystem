package com.hotelbookingsystem.controller;

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

        User user = authService.login(email, password);

        if (user == null) {
            ra.addFlashAttribute("error", "Sai email hoặc mật khẩu");
            return "redirect:/login";
        }

        session.setAttribute("user", user);
        return "redirect:/rooms";
    }
}
