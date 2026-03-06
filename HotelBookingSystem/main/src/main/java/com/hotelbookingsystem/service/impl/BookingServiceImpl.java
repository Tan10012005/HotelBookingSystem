package com.hotelbookingsystem.service.impl;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.enums.*;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.repository.RefundTransactionRepository;
import com.hotelbookingsystem.repository.RoomRepository;
import com.hotelbookingsystem.service.BookingService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private RefundTransactionRepository refundTransactionRepo;

    /**
     * Business Rule mới:
     * - Hủy >= 5 ngày trước check-in → hoàn 50%
     * - Hủy < 5 ngày trước check-in → không hoàn tiền (0%)
     */
    private static final int REFUND_CUTOFF_DAYS = 5;
    private static final Integer PARTIAL_REFUND_PERCENTAGE = 50;   // >= 5 ngày → 50%
    private static final Integer NO_REFUND_PERCENTAGE = 0;          // < 5 ngày → 0%
    private static final Integer FULL_REFUND_PERCENTAGE = 100;      // Giữ lại cho getRefundPercentage default

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
                .refundPercentage(FULL_REFUND_PERCENTAGE)
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
    public CancelResult cancelBooking(Long bookingId, User user,
                                      CancellationReason cancellationReason,
                                      String bankName, String accountNumber, String accountHolderName) {
        Optional<Booking> maybe = bookingRepo.findByIdAndUser(bookingId, user);

        if (maybe.isEmpty()) {
            return CancelResult.NOT_FOUND;
        }

        Booking booking = maybe.get();

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return CancelResult.ALREADY_CANCELLED;
        }

        // ========== BUSINESS RULE MỚI ==========
        // Tính số ngày từ bây giờ đến ngày check-in
        LocalDate today = LocalDate.now();
        LocalDate checkInDate = booking.getCheckIn();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(today, checkInDate);

        if (daysUntilCheckIn >= REFUND_CUTOFF_DAYS) {
            // Hủy trước >= 5 ngày → hoàn 50%
            booking.setRefundPercentage(PARTIAL_REFUND_PERCENTAGE);
        } else {
            // Hủy < 5 ngày trước check-in → KHÔNG hoàn tiền
            booking.setRefundPercentage(NO_REFUND_PERCENTAGE);
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal refundAmount = calculateRefundAmount(booking);
        booking.setRefundAmount(refundAmount);
        booking.setCancelledAt(now);
        booking.setCancellationReason(cancellationReason);
        booking.setStatus(BookingStatus.CANCELLED);

        // Chỉ tạo yêu cầu hoàn tiền nếu có tiền hoàn (> 0)
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            booking.setRefundStatus(RefundStatus.REQUESTED);

            // Tạo RefundTransaction với thông tin chuyển khoản
            RefundTransaction transaction = RefundTransaction.builder()
                    .booking(booking)
                    .user(user)
                    .bankName(bankName)
                    .accountNumber(accountNumber)
                    .accountHolderName(accountHolderName)
                    .refundAmount(refundAmount)
                    .refundPercentage(booking.getRefundPercentage())
                    .status(RefundTransactionStatus.PENDING)
                    .refundDeadline(now.plusHours(24))  // Hoàn trong 24h
                    .build();
            refundTransactionRepo.save(transaction);
        } else {
            // Không hoàn tiền
            booking.setRefundStatus(RefundStatus.NONE);
        }

        bookingRepo.save(booking);

        // Giải phóng phòng
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
    public boolean adminProcessRefundTransaction(Long transactionId, String adminNote) {
        Optional<RefundTransaction> maybe = refundTransactionRepo.findById(transactionId);
        if (maybe.isEmpty()) return false;

        RefundTransaction tx = maybe.get();

        // Chỉ xử lý transaction đang PENDING
        if (tx.getStatus() != RefundTransactionStatus.PENDING) return false;

        // Cập nhật transaction
        tx.setStatus(RefundTransactionStatus.COMPLETED);
        tx.setAdminNote(adminNote);
        tx.setProcessedAt(LocalDateTime.now());
        refundTransactionRepo.save(tx);

        // Cập nhật booking refund status
        Booking booking = tx.getBooking();
        booking.setRefundStatus(RefundStatus.TRANSFERRED);
        bookingRepo.save(booking);

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

    @Override
    public BigDecimal calculateRefundAmount(Booking booking) {
        if (booking.getTotalPrice() == null || booking.getRefundPercentage() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal refundPercentage = BigDecimal.valueOf(booking.getRefundPercentage());
        return booking.getTotalPrice()
                .multiply(refundPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public Integer getRefundPercentage(Booking booking) {
        if (booking == null || booking.getRefundPercentage() == null) {
            return FULL_REFUND_PERCENTAGE;
        }
        return booking.getRefundPercentage();
    }

    @Override
    public List<RefundTransaction> getPendingRefundTransactions() {
        return refundTransactionRepo.findByStatus(RefundTransactionStatus.PENDING);
    }

    @Override
    public List<RefundTransaction> getAllRefundTransactions() {
        return refundTransactionRepo.findAll();
    }
}