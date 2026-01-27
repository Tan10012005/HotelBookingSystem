package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.User;


public interface AuthService {
    User login(String email, String password);

}

