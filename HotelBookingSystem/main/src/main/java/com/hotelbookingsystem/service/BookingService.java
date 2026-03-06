package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.RefundTransaction;
import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.enums.CancelResult;
import com.hotelbookingsystem.enums.CancellationReason;

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

    /**
     * Hủy booking với business rule mới:
     * - Hủy >= 5 ngày trước check-in: hoàn 50%
     * - Hủy < 5 ngày trước check-in: không hoàn tiền (0%)
     * - Yêu cầu thông tin chuyển khoản để tạo RefundTransaction
     */
    CancelResult cancelBooking(Long bookingId, User user,
                               CancellationReason cancellationReason,
                               String bankName, String accountNumber, String accountHolderName);

    boolean adminMarkRefundTransferred(Long bookingId);

    /**
     * Admin xử lý giao dịch hoàn tiền - thực hiện chuyển khoản
     */
    boolean adminProcessRefundTransaction(Long transactionId, String adminNote);

    boolean userConfirmRefundReceived(Long bookingId, User user);

    BigDecimal calculateRefundAmount(Booking booking);

    Integer getRefundPercentage(Booking booking);

    /**
     * Lấy tất cả RefundTransaction đang chờ xử lý (cho admin)
     */
    List<RefundTransaction> getPendingRefundTransactions();

    /**
     * Lấy tất cả RefundTransaction (cho admin)
     */
    List<RefundTransaction> getAllRefundTransactions();
}