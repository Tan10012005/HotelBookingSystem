package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.enums.BookingStatus;
import com.hotelbookingsystem.enums.CheckInStatus;
import com.hotelbookingsystem.enums.RoomChangeStatus;
import com.hotelbookingsystem.enums.RoomStatus;
import com.hotelbookingsystem.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomChangeServiceImpl implements RoomChangeService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomChangeRequestRepository roomChangeRequestRepository;

    // ===== User-side methods =====

    @Override
    public List<Booking> getEligibleBookingsForChange(User user) {
        return bookingRepository.findByUser(user).stream()
                .filter(b -> {
                    // Chỉ booking đã CONFIRMED
                    if (b.getStatus() != BookingStatus.CONFIRMED) return false;
                    // Chưa check-in
                    if (b.getCheckInStatus() != CheckInStatus.PENDING) return false;
                    // Ngày check-in phải > hôm nay (còn ít nhất 1 ngày để đổi)
                    if (!b.getCheckIn().isAfter(LocalDate.now())) return false;
                    // Chưa có yêu cầu đổi phòng đang PENDING
                    if (roomChangeRequestRepository.existsByBookingAndStatus(b, RoomChangeStatus.PENDING)) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Room> getAvailableRoomsForUpgrade(Room currentRoom) {
        return roomRepository.findByStatus(RoomStatus.AVAILABLE).stream()
                .filter(r -> !r.getId().equals(currentRoom.getId()))
                // Chỉ hiện phòng cùng giá hoặc cao hơn (upgrade / ngang hạng)
                .filter(r -> r.getPricePerNight().compareTo(currentRoom.getPricePerNight()) >= 0)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculatePriceDifference(Booking booking, Room newRoom) {
        long nights = booking.getCheckOut().toEpochDay() - booking.getCheckIn().toEpochDay();
        BigDecimal oldTotal = booking.getRoom().getPricePerNight().multiply(BigDecimal.valueOf(nights));
        BigDecimal newTotal = newRoom.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        return newTotal.subtract(oldTotal);
    }

    @Override
    @Transactional
    public Optional<RoomChangeRequest> createRequest(Long bookingId, Long newRoomId, User user) {
        // Tìm booking
        Optional<Booking> maybeBooking = bookingRepository.findByIdAndUser(bookingId, user);
        if (maybeBooking.isEmpty()) return Optional.empty();

        Booking booking = maybeBooking.get();

        // Kiểm tra tính hợp lệ
        if (booking.getStatus() != BookingStatus.CONFIRMED) return Optional.empty();
        if (booking.getCheckInStatus() != CheckInStatus.PENDING) return Optional.empty();
        if (!booking.getCheckIn().isAfter(LocalDate.now())) return Optional.empty();
        if (roomChangeRequestRepository.existsByBookingAndStatus(booking, RoomChangeStatus.PENDING)) {
            return Optional.empty(); // Đã có yêu cầu đang chờ
        }

        // Tìm phòng mới
        Optional<Room> maybeRoom = roomRepository.findById(newRoomId);
        if (maybeRoom.isEmpty()) return Optional.empty();

        Room newRoom = maybeRoom.get();
        if (newRoom.getStatus() != RoomStatus.AVAILABLE) return Optional.empty();
        if (newRoom.getId().equals(booking.getRoom().getId())) return Optional.empty();
        // Chỉ cho phép upgrade hoặc ngang hạng
        if (newRoom.getPricePerNight().compareTo(booking.getRoom().getPricePerNight()) < 0) {
            return Optional.empty();
        }

        BigDecimal priceDiff = calculatePriceDifference(booking, newRoom);

        RoomChangeRequest request = RoomChangeRequest.builder()
                .booking(booking)
                .user(user)
                .oldRoom(booking.getRoom())
                .newRoom(newRoom)
                .priceDifference(priceDiff)
                .status(RoomChangeStatus.PENDING)
                .build();

        return Optional.of(roomChangeRequestRepository.save(request));
    }

    // ===== Admin-side methods =====

    @Override
    @Transactional
    public boolean approveRequest(Long requestId) {
        Optional<RoomChangeRequest> maybe = roomChangeRequestRepository.findById(requestId);
        if (maybe.isEmpty()) return false;

        RoomChangeRequest req = maybe.get();
        if (req.getStatus() != RoomChangeStatus.PENDING) return false;

        Booking booking = req.getBooking();
        Room oldRoom = req.getOldRoom();
        Room newRoom = req.getNewRoom();

        // Kiểm tra phòng mới vẫn còn trống
        if (newRoom.getStatus() != RoomStatus.AVAILABLE) return false;

        // Cập nhật tổng giá booking
        long nights = booking.getCheckOut().toEpochDay() - booking.getCheckIn().toEpochDay();
        BigDecimal newTotalPrice = newRoom.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        booking.setTotalPrice(newTotalPrice);

        // Gán phòng mới vào booking
        booking.setRoom(newRoom);
        bookingRepository.save(booking);

        // Phòng cũ → AVAILABLE
        oldRoom.setStatus(RoomStatus.AVAILABLE);
        roomRepository.save(oldRoom);

        // Phòng mới → BOOKED
        newRoom.setStatus(RoomStatus.BOOKED);
        roomRepository.save(newRoom);

        // Cập nhật request → APPROVED
        req.setStatus(RoomChangeStatus.APPROVED);
        roomChangeRequestRepository.save(req);

        return true;
    }

    @Override
    @Transactional
    public boolean rejectRequest(Long requestId, String adminNote) {
        Optional<RoomChangeRequest> maybe = roomChangeRequestRepository.findById(requestId);
        if (maybe.isEmpty()) return false;

        RoomChangeRequest req = maybe.get();
        if (req.getStatus() != RoomChangeStatus.PENDING) return false;

        req.setStatus(RoomChangeStatus.REJECTED);
        req.setAdminNote(adminNote);
        roomChangeRequestRepository.save(req);

        return true;
    }

    @Override
    public List<RoomChangeRequest> getAllRequests() {
        return roomChangeRequestRepository.findAll();
    }

    @Override
    public List<RoomChangeRequest> getRequestsByUser(User user) {
        return roomChangeRequestRepository.findByUser(user);
    }
}