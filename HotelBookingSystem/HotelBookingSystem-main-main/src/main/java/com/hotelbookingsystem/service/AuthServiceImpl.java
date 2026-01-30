package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Admin;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.repository.AdminRepository;
import com.hotelbookingsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return null;
        }

        if (!user.getPassword().equals(password)) {
            return null;
        }

        return user;
    }

    @Override
    public User register(String email, String password) {
        if (userRepository.findByEmail(email) != null) {
            return null;
        }

        User user = User.builder()
                .email(email)
                .password(password)
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    @Override
    public Admin adminLogin(String email, String password) {
        Admin admin = adminRepository.findByEmail(email);

        if (admin == null) {
            return null;
        }

        if (!admin.getPassword().equals(password)) {
            return null;
        }

        if (!admin.getIsActive()) {
            return null;
        }

        return admin;
    }
}