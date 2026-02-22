package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.entity.CancelResult;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private RoomRepository roomRepo;

    // 🆕 Constants for refund policy
    private static final int REFUND_WINDOW_HOURS = 24;
    private static final Integer FULL_REFUND_PERCENTAGE = 100;
    private static final Integer PARTIAL_REFUND_PERCENTAGE = 50;

    @Override
    public void createBooking(User user, Room room,
                              LocalDate checkIn, LocalDate checkOut, int guests) {

        long days = checkOut.toEpochDay() - checkIn.toEpochDay();
        BigDecimal total = room.getPricePerNight()
                .multiply(BigDecimal.valueOf(days));

        Booking booking = Booking.builder()
                .user(user)
                .room(room)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(guests)
                .totalPrice(total)
                .status(BookingStatus.PENDING_CONFIRM)
                .refundStatus(RefundStatus.NONE)
                .refundPercentage(FULL_REFUND_PERCENTAGE) // Default 100%
                .build();

        bookingRepo.save(booking);

        room.setStatus(RoomStatus.BOOKED);
        roomRepo.save(room);
    }

    @Override
    public java.util.List<Booking> getBookingsByUser(User user) {
        return bookingRepo.findByUserId(user.getId());
    }

    @Override
    @Transactional
    public CancelResult cancelBooking(Long bookingId, User user) {
        Optional<Booking> maybe = bookingRepo.findByIdAndUser(bookingId, user);

        if (maybe.isEmpty()) {
            return CancelResult.NOT_FOUND;
        }

        Booking booking = maybe.get();

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return CancelResult.ALREADY_CANCELLED;
        }

        // ✅ UPDATED LOGIC: Calculate refund based on current time vs check-in time
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInDateTime = booking.getCheckIn().atStartOfDay();
        LocalDateTime refundCutoffTime = checkInDateTime.minusHours(REFUND_WINDOW_HOURS);

        // Determine refund percentage based on cancellation time
        if (now.isAfter(refundCutoffTime)) {
            // Within 24 hours of check-in → 50% refund
            booking.setRefundPercentage(PARTIAL_REFUND_PERCENTAGE);
        } else {
            // More than 24 hours before check-in → 100% refund
            booking.setRefundPercentage(FULL_REFUND_PERCENTAGE);
        }

        // Calculate refund amount
        BigDecimal refundAmount = calculateRefundAmount(booking);
        booking.setRefundAmount(refundAmount);
        booking.setCancelledAt(now);

        // Mark cancelled and request refund
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setRefundStatus(RefundStatus.REQUESTED);
        bookingRepo.save(booking);

        Room room = booking.getRoom();
        if (room != null && room.getStatus() == RoomStatus.BOOKED) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepo.save(room);
        }

        return CancelResult.SUCCESS;
    }

    @Override
    @Transactional
    public boolean adminMarkRefundTransferred(Long bookingId) {
        Optional<Booking> maybe = bookingRepo.findById(bookingId);
        if (maybe.isEmpty()) return false;
        Booking b = maybe.get();

        if (b.getStatus() != BookingStatus.CANCELLED) return false;
        if (b.getRefundStatus() != RefundStatus.REQUESTED) return false;

        b.setRefundStatus(RefundStatus.TRANSFERRED);
        bookingRepo.save(b);
        return true;
    }

    @Override
    @Transactional
    public boolean userConfirmRefundReceived(Long bookingId, User user) {
        Optional<Booking> maybe = bookingRepo.findByIdAndUser(bookingId, user);
        if (maybe.isEmpty()) return false;
        Booking b = maybe.get();

        if (b.getRefundStatus() != RefundStatus.TRANSFERRED) return false;

        b.setRefundStatus(RefundStatus.RECEIVED);
        bookingRepo.save(b);
        return true;
    }

    // 🆕 NEW METHOD: Calculate refund amount based on refund percentage
    @Override
    public BigDecimal calculateRefundAmount(Booking booking) {
        if (booking.getTotalPrice() == null || booking.getRefundPercentage() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal refundPercentage = BigDecimal.valueOf(booking.getRefundPercentage());
        return booking.getTotalPrice()
                .multiply(refundPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // 🆕 NEW METHOD: Get refund percentage
    @Override
    public Integer getRefundPercentage(Booking booking) {
        if (booking == null || booking.getRefundPercentage() == null) {
            return FULL_REFUND_PERCENTAGE;
        }
        return booking.getRefundPercentage();
    }
}