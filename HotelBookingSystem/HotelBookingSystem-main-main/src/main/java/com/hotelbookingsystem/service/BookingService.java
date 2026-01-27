package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    void createBooking(
            User user,
            Room room,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests
    );

    List<Booking> getBookingsByUser(User user);

    boolean cancelBooking(Long bookingId, User user);
}
