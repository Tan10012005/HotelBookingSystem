package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.enums.CancelResult;
import com.hotelbookingsystem.enums.CancellationReason;
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

    // 🆕 UPDATED: Add cancellationReason parameter
    CancelResult cancelBooking(Long bookingId, User user, CancellationReason cancellationReason);

    boolean adminMarkRefundTransferred(Long bookingId);

    boolean userConfirmRefundReceived(Long bookingId, User user);

    BigDecimal calculateRefundAmount(Booking booking);

    Integer getRefundPercentage(Booking booking);
}