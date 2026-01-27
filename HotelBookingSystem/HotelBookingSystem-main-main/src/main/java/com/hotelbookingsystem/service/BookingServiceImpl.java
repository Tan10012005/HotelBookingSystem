package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    public boolean cancelBooking(Long bookingId, User user) {
        Optional<Booking> maybe = bookingRepo.findByIdAndUser(bookingId, user);

        if (maybe.isEmpty()) {
            return false;
        }

        Booking booking = maybe.get();

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return false;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        Room room = booking.getRoom();
        if (room != null && room.getStatus() == RoomStatus.BOOKED) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepo.save(room);
        }

        return true;
    }
}