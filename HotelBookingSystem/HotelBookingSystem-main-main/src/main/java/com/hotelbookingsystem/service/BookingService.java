package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.CancelResult;
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

    // Changed: return CancelResult to indicate reason
    CancelResult cancelBooking(Long bookingId, User user);

    // Admin marks that money has been transferred to user
    boolean adminMarkRefundTransferred(Long bookingId);

    // User confirms they received the refund
    boolean userConfirmRefundReceived(Long bookingId, User user);
}