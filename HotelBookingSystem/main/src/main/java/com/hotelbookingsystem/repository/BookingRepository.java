package com.hotelbookingsystem.repository;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.enums.BookingStatus;
import com.hotelbookingsystem.enums.CheckInStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT b FROM Booking b
        WHERE b.room = :room
        AND b.status = 'CONFIRMED'
        AND :checkIn < b.checkOut
        AND :checkOut > b.checkIn
    """)
    List<Booking> findOverlappingBookings(
            Room room,
            LocalDate checkIn,
            LocalDate checkOut
    );

    List<Booking> findByUserId(Long userId);

    List<Booking> findByStatus(BookingStatus status);

    // Tìm booking theo ID và User
    Optional<Booking> findByIdAndUser(Long id, User user);

    // Tìm booking theo User
    List<Booking> findByUser(User user);

    // ========== 🆕 THÊM CÁC METHOD CHO WORKFLOW 3 ==========

    /** Tìm booking theo QR code */
    Optional<Booking> findByQrCode(String qrCode);

    /** Tìm tất cả booking có trạng thái check-in là PENDING */
    List<Booking> findByCheckInStatus(CheckInStatus status);

    /** Tìm booking theo user + trạng thái check-in */
    List<Booking> findByUserAndCheckInStatus(User user, CheckInStatus status);
}