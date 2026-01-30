package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Admin;
import com.hotelbookingsystem.entity.User;


public interface AuthService {
    User login(String email, String password);

    User register(String email, String password);
    Admin adminLogin(String email, String password);

}

