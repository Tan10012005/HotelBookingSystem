package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.RefundTransaction;
import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.entity.Wallet;
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
     * Hủy booking với business rule 3 mức:
     * - Hủy >= 7 ngày trước check-in: hoàn 100%
     * - Hủy >= 3 ngày và < 7 ngày: hoàn 50%
     * - Hủy < 3 ngày: không hoàn tiền (0%)
     * - Yêu cầu thông tin chuyển khoản để tạo RefundTransaction
     */
    CancelResult cancelBooking(Long bookingId, User user,
                               CancellationReason cancellationReason,
                               String bankName, String accountNumber, String accountHolderName);

    boolean adminMarkRefundTransferred(Long bookingId);

    /**
     * Admin xử lý giao dịch hoàn tiền:
     * - Cộng tiền vào ví user
     * - Cập nhật trạng thái RefundTransaction → COMPLETED
     * - Cập nhật RefundStatus booking → RECEIVED
     */
    boolean adminProcessRefundTransaction(Long transactionId, String adminNote);

    boolean userConfirmRefundReceived(Long bookingId, User user);

    BigDecimal calculateRefundAmount(Booking booking);

    Integer getRefundPercentage(Booking booking);

    /** Lấy tất cả RefundTransaction đang chờ xử lý (cho admin) */
    List<RefundTransaction> getPendingRefundTransactions();

    /** Lấy tất cả RefundTransaction (cho admin) */
    List<RefundTransaction> getAllRefundTransactions();

    /**
     * Lấy ví của user, tạo mới nếu chưa có
     */
    Wallet getOrCreateWallet(User user);
}