package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

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
        // Nếu đã tồn tại user cùng email hay ko
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
}
