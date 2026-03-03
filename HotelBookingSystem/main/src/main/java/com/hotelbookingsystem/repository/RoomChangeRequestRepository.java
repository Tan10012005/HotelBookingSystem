package com.hotelbookingsystem.repository;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.RoomChangeRequest;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.enums.RoomChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomChangeRequestRepository extends JpaRepository<RoomChangeRequest, Long> {

    /** Tìm tất cả yêu cầu đổi phòng của một user */
    List<RoomChangeRequest> findByUser(User user);

    /** Tìm tất cả yêu cầu theo trạng thái */
    List<RoomChangeRequest> findByStatus(RoomChangeStatus status);

    /** Kiểm tra booking đã có yêu cầu đổi phòng đang PENDING chưa */
    boolean existsByBookingAndStatus(Booking booking, RoomChangeStatus status);

    /** Tìm yêu cầu theo ID và user (kiểm tra quyền) */
    Optional<RoomChangeRequest> findByIdAndUser(Long id, User user);
}