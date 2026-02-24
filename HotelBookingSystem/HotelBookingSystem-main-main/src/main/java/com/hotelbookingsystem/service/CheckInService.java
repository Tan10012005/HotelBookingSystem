package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.User;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý check-in online
 */
public interface CheckInService {

    /**
     * Lấy danh sách booking sắp tới (có thể check-in ngay)
     * Chỉ lấy các booking có check-in trong 7 ngày tới và trạng thái CONFIRMED
     */
    List<Booking> getUpcomingBookings(User user);

    /**
     * Kiểm tra xem booking có sẵn sàng check-in không
     */
    boolean isEligibleForCheckIn(Booking booking);

    /**
     * Tạo QR code cho booking
     */
    String generateQRCode(Booking booking);

    /**
     * Xử lý check-in online: tạo QR code và lưu trạng thái
     */
    Optional<Booking> performOnlineCheckIn(Long bookingId, User user, String citizenId, String notes);

    /**
     * Lấy booking bằng ID + User (kiểm tra quyền)
     */
    Optional<Booking> getBookingByIdAndUser(Long bookingId, User user);

    /**
     * Cập nhật trạng thái check-in sau khi quét QR
     */
    void confirmCheckInByQR(String qrCode);
}