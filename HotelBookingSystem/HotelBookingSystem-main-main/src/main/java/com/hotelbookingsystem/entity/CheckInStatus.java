package com.hotelbookingsystem.entity;

/**
 * Trạng thái check-in của booking
 */
public enum CheckInStatus {
    PENDING,      // Chưa check-in
    CHECKED_IN,   // Đã check-in
    CHECKED_OUT   // Đã check-out
}