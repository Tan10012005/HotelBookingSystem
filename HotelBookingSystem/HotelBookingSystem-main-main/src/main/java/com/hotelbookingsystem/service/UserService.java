package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.User;

public interface UserService {
    User getUserById(Long id);
    User updateProfile(Long userId, String fullName, String phoneNumber, String citizenId);
}