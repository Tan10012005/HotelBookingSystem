package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.CancelResult;
import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.User;

import java.math.BigDecimal;
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

    CancelResult cancelBooking(Long bookingId, User user);

    boolean adminMarkRefundTransferred(Long bookingId);

    boolean userConfirmRefundReceived(Long bookingId, User user);

    // 🆕 NEW METHOD: Calculate refund amount based on cancellation time
    BigDecimal calculateRefundAmount(Booking booking);

    // 🆕 NEW METHOD: Get refund percentage for a booking
    Integer getRefundPercentage(Booking booking);
}