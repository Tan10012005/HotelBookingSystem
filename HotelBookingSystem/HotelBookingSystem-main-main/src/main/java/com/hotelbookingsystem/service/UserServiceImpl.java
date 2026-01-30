package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User updateProfile(Long userId, String fullName, String phoneNumber, String citizenId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return null;
        }

        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        user.setCitizenId(citizenId);

        return userRepository.save(user);
    }
}