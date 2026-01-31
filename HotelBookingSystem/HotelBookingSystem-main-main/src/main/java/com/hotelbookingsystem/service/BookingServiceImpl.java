package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.entity.CancelResult;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private RoomRepository roomRepo;

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

        // Validate: cannot cancel if within 24 hours of check-in
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = booking.getCheckIn().atStartOfDay().minusHours(24);
        if (cutoff.isBefore(now) || cutoff.isEqual(now)) {
            return CancelResult.TOO_LATE;
        }

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
}