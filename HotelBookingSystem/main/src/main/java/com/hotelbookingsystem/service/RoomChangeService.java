package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.RoomChangeRequest;
import com.hotelbookingsystem.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RoomChangeService {

    /**
     * Lấy danh sách booking hợp lệ để đổi phòng:
     * - Trạng thái CONFIRMED
     * - Chưa check-in
     * - Ngày check-in > hôm nay (còn ít nhất 1 ngày)
     */
    List<Booking> getEligibleBookingsForChange(User user);

    /**
     * Lấy danh sách phòng có thể đổi sang:
     * - Trạng thái AVAILABLE
     * - Không phải phòng hiện tại
     * - Giá >= giá phòng hiện tại (chỉ upgrade / ngang hạng)
     */
    List<Room> getAvailableRoomsForUpgrade(Room currentRoom);

    /**
     * Tính chênh lệch giá giữa phòng cũ và phòng mới
     * = (newPrice - oldPrice) * số đêm
     */
    BigDecimal calculatePriceDifference(Booking booking, Room newRoom);

    /**
     * Tạo yêu cầu đổi phòng và lưu vào DB.
     * Trả về Optional.empty() nếu không hợp lệ.
     */
    Optional<RoomChangeRequest> createRequest(Long bookingId, Long newRoomId, User user);

    /**
     * Admin duyệt yêu cầu:
     * - Cập nhật booking sang phòng mới
     * - Cập nhật giá booking
     * - Phòng cũ → AVAILABLE
     * - Phòng mới → BOOKED
     * - Request → APPROVED
     */
    boolean approveRequest(Long requestId);

    /**
     * Admin từ chối yêu cầu:
     * - Request → REJECTED
     * - Lưu adminNote
     */
    boolean rejectRequest(Long requestId, String adminNote);

    /** Tất cả yêu cầu (cho admin) */
    List<RoomChangeRequest> getAllRequests();

    /** Yêu cầu của một user */
    List<RoomChangeRequest> getRequestsByUser(User user);
}