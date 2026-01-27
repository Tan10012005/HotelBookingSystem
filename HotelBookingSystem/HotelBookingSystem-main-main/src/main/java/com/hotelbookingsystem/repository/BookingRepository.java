package com.hotelbookingsystem.repository;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.BookingStatus;
import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
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

    // ✅ THÊM MỚI: Tìm booking theo ID và User (để check ownership)
    Optional<Booking> findByIdAndUser(Long id, User user);

    // ✅ THÊM MỚI: Tìm booking theo User (có thể dùng thay cho findByUserId)
    List<Booking> findByUser(User user);
}
