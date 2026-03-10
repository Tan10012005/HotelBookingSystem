package com.hotelbookingsystem.service.impl;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.enums.*;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.repository.RefundTransactionRepository;
import com.hotelbookingsystem.repository.RoomRepository;
import com.hotelbookingsystem.repository.WalletRepository;
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

    @Autowired
    private WalletRepository walletRepo;

    // =========================================================
    //  Business Rule hoàn tiền (3 mức):
    //  - Hủy >= 7 ngày trước check-in → hoàn 100%
    //  - Hủy >= 3 ngày và < 7 ngày   → hoàn 50%
    //  - Hủy < 3 ngày                → mất toàn bộ (0%)
    // =========================================================
    private static final int FULL_REFUND_DAYS        = 7;
    private static final int PARTIAL_REFUND_DAYS     = 3;
    private static final Integer FULL_REFUND_PERCENTAGE    = 100;
    private static final Integer PARTIAL_REFUND_PERCENTAGE = 50;
    private static final Integer NO_REFUND_PERCENTAGE      = 0;

    // =========================================================
    //  CREATE BOOKING
    // =========================================================
    @Override
    public void createBooking(User user, Room room,
                              LocalDate checkIn, LocalDate checkOut, int guests) {
        long days = checkOut.toEpochDay() - checkIn.toEpochDay();
        BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(days));

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

    // =========================================================
    //  GET BOOKINGS BY USER
    // =========================================================
    @Override
    public List<Booking> getBookingsByUser(User user) {
        return bookingRepo.findByUserId(user.getId());
    }

    // =========================================================
    //  CANCEL BOOKING
    // =========================================================
    @Override
    @Transactional
    public CancelResult cancelBooking(Long bookingId, User user,
                                      CancellationReason cancellationReason,
                                      String bankName, String accountNumber,
                                      String accountHolderName) {

        Optional<Booking> maybe = bookingRepo.findByIdAndUser(bookingId, user);
        if (maybe.isEmpty()) return CancelResult.NOT_FOUND;

        Booking booking = maybe.get();
        if (booking.getStatus() == BookingStatus.CANCELLED) return CancelResult.ALREADY_CANCELLED;

        // ── Xác định mức hoàn tiền ──
        LocalDate today         = LocalDate.now();
        long daysUntilCheckIn   = ChronoUnit.DAYS.between(today, booking.getCheckIn());

        if (daysUntilCheckIn >= FULL_REFUND_DAYS) {
            booking.setRefundPercentage(FULL_REFUND_PERCENTAGE);
        } else if (daysUntilCheckIn >= PARTIAL_REFUND_DAYS) {
            booking.setRefundPercentage(PARTIAL_REFUND_PERCENTAGE);
        } else {
            booking.setRefundPercentage(NO_REFUND_PERCENTAGE);
        }

        LocalDateTime now        = LocalDateTime.now();
        BigDecimal refundAmount  = calculateRefundAmount(booking);

        booking.setRefundAmount(refundAmount);
        booking.setCancelledAt(now);
        booking.setCancellationReason(cancellationReason);
        booking.setStatus(BookingStatus.CANCELLED);

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Có hoàn tiền → tạo RefundTransaction chờ admin xử lý
            booking.setRefundStatus(RefundStatus.REQUESTED);

            RefundTransaction transaction = RefundTransaction.builder()
                    .booking(booking)
                    .user(user)
                    .bankName(bankName)
                    .accountNumber(accountNumber)
                    .accountHolderName(accountHolderName)
                    .refundAmount(refundAmount)
                    .refundPercentage(booking.getRefundPercentage())
                    .status(RefundTransactionStatus.PENDING)
                    .refundDeadline(now.plusHours(24))
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

    // =========================================================
    //  ADMIN: Đánh dấu đã chuyển khoản hoàn tiền (legacy)
    // =========================================================
    @Override
    @Transactional
    public boolean adminMarkRefundTransferred(Long bookingId) {
        Optional<Booking> maybe = bookingRepo.findById(bookingId);
        if (maybe.isEmpty()) return false;

        Booking b = maybe.get();
        if (b.getStatus() != BookingStatus.CANCELLED)       return false;
        if (b.getRefundStatus() != RefundStatus.REQUESTED)  return false;

        b.setRefundStatus(RefundStatus.TRANSFERRED);
        bookingRepo.save(b);
        return true;
    }

    // =========================================================
    //  ADMIN: Xử lý RefundTransaction → cộng tiền vào ví user
    // =========================================================
    @Override
    @Transactional
    public boolean adminProcessRefundTransaction(Long transactionId, String adminNote) {
        Optional<RefundTransaction> maybe = refundTransactionRepo.findById(transactionId);
        if (maybe.isEmpty()) return false;

        RefundTransaction tx = maybe.get();
        if (tx.getStatus() != RefundTransactionStatus.PENDING) return false;

        // ✅ Cộng tiền vào ví user
        User user    = tx.getUser();
        Wallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance().add(tx.getRefundAmount()));
        walletRepo.save(wallet);

        // Cập nhật trạng thái RefundTransaction
        tx.setStatus(RefundTransactionStatus.COMPLETED);
        tx.setAdminNote(adminNote);
        tx.setProcessedAt(LocalDateTime.now());
        refundTransactionRepo.save(tx);

        // Cập nhật trạng thái booking → tiền đã vào ví
        Booking booking = tx.getBooking();
        booking.setRefundStatus(RefundStatus.RECEIVED);
        bookingRepo.save(booking);

        return true;
    }

    // =========================================================
    //  USER: Xác nhận đã nhận tiền hoàn
    // =========================================================
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

    // =========================================================
    //  HELPER: Tính số tiền hoàn
    // =========================================================
    @Override
    public BigDecimal calculateRefundAmount(Booking booking) {
        if (booking.getTotalPrice() == null || booking.getRefundPercentage() == null) {
            return BigDecimal.ZERO;
        }
        return booking.getTotalPrice()
                .multiply(BigDecimal.valueOf(booking.getRefundPercentage()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public Integer getRefundPercentage(Booking booking) {
        if (booking == null || booking.getRefundPercentage() == null) {
            return FULL_REFUND_PERCENTAGE;
        }
        return booking.getRefundPercentage();
    }

    // =========================================================
    //  REFUND TRANSACTIONS
    // =========================================================
    @Override
    public List<RefundTransaction> getPendingRefundTransactions() {
        return refundTransactionRepo.findByStatus(RefundTransactionStatus.PENDING);
    }

    @Override
    public List<RefundTransaction> getAllRefundTransactions() {
        return refundTransactionRepo.findAll();
    }

    // =========================================================
    //  WALLET HELPER
    // =========================================================

    /**
     * Lấy ví của user, tự động tạo mới nếu chưa tồn tại.
     * Dùng chung cho AdminController hoặc bất kỳ nơi nào cần wallet.
     */
    @Override
    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepo.findByUser(user).orElseGet(() -> {
            Wallet w = Wallet.builder()
                    .user(user)
                    .balance(BigDecimal.ZERO)
                    .build();
            return walletRepo.save(w);
        });
    }
}