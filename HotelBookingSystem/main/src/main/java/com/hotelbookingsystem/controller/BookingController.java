package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.RoomChangeRequest;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.enums.CancelResult;
import com.hotelbookingsystem.enums.CancellationReason;
import com.hotelbookingsystem.enums.RoomChangeStatus;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.repository.RoomChangeRequestRepository;
import com.hotelbookingsystem.service.BookingService;
import com.hotelbookingsystem.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private RoomChangeRequestRepository roomChangeRequestRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private BookingRepository bookingRepository;

    /**
     * Bước 1: Xem trước thông tin đặt phòng
     */
    @PostMapping("/preview")
    public String previewBooking(
            @RequestParam Long roomId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam int guests,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null || !user.isProfileComplete()) {
            ra.addFlashAttribute("error", "Vui lòng cập nhật đầy đủ thông tin hồ sơ (Họ tên, SĐT, CCCD) trước khi đặt phòng!");
            ra.addFlashAttribute("redirectUrl", "/rooms");
            return "redirect:/profile";
        }

        Room room = roomService.getRoomById(roomId);

        long days = checkOut.toEpochDay() - checkIn.toEpochDay();
        BigDecimal totalPrice = room.getPricePerNight()
                .multiply(BigDecimal.valueOf(days));

        model.addAttribute("room", room);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("guests", guests);
        model.addAttribute("totalPrice", totalPrice);

        return "bookingConfirm";
    }

    /**
     * Bước 2: Xác nhận đặt phòng và thanh toán
     */
    @PostMapping("/confirm")
    public String confirmBooking(
            @RequestParam Long roomId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam int guests,
            @RequestParam BigDecimal totalPrice,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null || !user.isProfileComplete()) {
            ra.addFlashAttribute("error", "Vui lòng cập nhật hồ sơ trước khi đặt phòng!");
            return "redirect:/profile";
        }

        Room room = roomService.getRoomById(roomId);
        bookingService.createBooking(user, room, checkIn, checkOut, guests);

        model.addAttribute("room", room);
        model.addAttribute("totalPrice", totalPrice);

        return "vietqr";
    }

    /**
     * Bước 3: Thông báo đặt phòng thành công
     */
    @GetMapping("/success")
    public String success() {
        return "bookingSuccess";
    }

    /**
     * Xem danh sách booking của tôi
     */
    @GetMapping("/my")
    public String myBookings(HttpSession session, Model model, RedirectAttributes ra) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        List<Booking> bookings = bookingService.getBookingsByUser(user);

        java.util.Map<Long, RoomChangeRequest> pendingChangeMap = new java.util.HashMap<>();
        roomChangeRequestRepository.findByUser(user).stream()
                .filter(r -> r.getStatus() == RoomChangeStatus.PENDING)
                .forEach(r -> pendingChangeMap.put(r.getBooking().getId(), r));

        model.addAttribute("bookings", bookings);
        model.addAttribute("pendingChangeMap", pendingChangeMap);
        return "bookingList";
    }

    /**
     * Hủy đơn đặt phòng — business rule 3 mức:
     * - >= 7 ngày trước check-in: hoàn 100%
     * - >= 3 ngày trước check-in: hoàn 50%
     * - < 3 ngày trước check-in (dưới 24h tính luôn): mất toàn bộ
     * - User phải nhập thông tin CK nếu được hoàn tiền (>= 3 ngày)
     */
    @PostMapping("/{id}/cancel")
    public String cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String accountHolderName,
            HttpSession session,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        CancellationReason cancellationReason = CancellationReason.OTHER;
        try {
            if (reason != null && !reason.isEmpty()) {
                cancellationReason = CancellationReason.valueOf(reason);
            }
        } catch (IllegalArgumentException ignored) {
            cancellationReason = CancellationReason.OTHER;
        }

        // Validate thông tin chuyển khoản — chỉ bắt buộc nếu >= 3 ngày (có hoàn tiền)
        Optional<Booking> checkBooking = bookingRepository.findByIdAndUser(id, user);
        if (checkBooking.isPresent()) {
            Booking bk = checkBooking.get();
            long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), bk.getCheckIn());

            // >= 3 ngày → có hoàn tiền → bắt buộc nhập thông tin CK
            if (daysUntilCheckIn >= 3) {
                if (bankName == null || bankName.trim().isEmpty()
                        || accountNumber == null || accountNumber.trim().isEmpty()
                        || accountHolderName == null || accountHolderName.trim().isEmpty()) {
                    ra.addFlashAttribute("error", "Vui lòng cung cấp đầy đủ thông tin tài khoản ngân hàng để chúng tôi xử lý hoàn tiền cho Quý khách.");
                    return "redirect:/booking/my";
                }
            }
        }

        if (bankName == null || bankName.trim().isEmpty()) bankName = "";
        if (accountNumber == null || accountNumber.trim().isEmpty()) accountNumber = "";
        if (accountHolderName == null || accountHolderName.trim().isEmpty()) accountHolderName = "";

        CancelResult result = bookingService.cancelBooking(
                id, user, cancellationReason,
                bankName.trim(), accountNumber.trim(), accountHolderName.trim()
        );

        switch (result) {
            case SUCCESS:
                Optional<Booking> booking = bookingRepository.findByIdAndUser(id, user);
                if (booking.isPresent()) {
                    Booking b = booking.get();
                    Integer refundPercentage = b.getRefundPercentage();
                    BigDecimal refundAmount = b.getRefundAmount();

                    String reasonText = b.getCancellationReason() != null
                            ? b.getCancellationReason().getLabel()
                            : "Không xác định";

                    if (refundPercentage != null && refundPercentage > 0) {
                        String message = String.format(
                                "Yêu cầu hủy đặt phòng đã được xử lý thành công.\n" +
                                        "Lý do hủy: %s\n" +
                                        "Mức hoàn tiền: %d%% — Số tiền hoàn: %,.0f VND\n" +
                                        "Khoản hoàn tiền sẽ được chuyển vào tài khoản của Quý khách trong vòng 24 giờ làm việc.\n" +
                                        "Bộ phận hỗ trợ sẽ xử lý giao dịch trong thời gian sớm nhất.",
                                reasonText,
                                refundPercentage,
                                refundAmount != null ? refundAmount.doubleValue() : 0
                        );
                        ra.addFlashAttribute("message", message);
                    } else {
                        String message = String.format(
                                "Yêu cầu hủy đặt phòng đã được xử lý thành công.\n" +
                                        "Lý do hủy: %s\n" +
                                        "Theo chính sách hủy phòng, yêu cầu hủy trong vòng 3 ngày trước ngày nhận phòng sẽ không được hoàn tiền.",
                                reasonText
                        );
                        ra.addFlashAttribute("message", message);
                    }
                } else {
                    ra.addFlashAttribute("message", "Yêu cầu hủy đặt phòng đã được xử lý thành công.");
                }
                break;
            case NOT_FOUND:
                ra.addFlashAttribute("error", "Không tìm thấy thông tin đặt phòng. Vui lòng thử lại hoặc liên hệ bộ phận hỗ trợ.");
                break;
            case ALREADY_CANCELLED:
                ra.addFlashAttribute("error", "Đặt phòng này đã được hủy trước đó.");
                break;
            case TOO_LATE:
                ra.addFlashAttribute("error", "Không thể hủy đặt phòng. Yêu cầu hủy đã quá thời hạn cho phép.");
                break;
            default:
                ra.addFlashAttribute("error", "Không thể xử lý yêu cầu hủy đặt phòng. Vui lòng thử lại sau.");
        }

        return "redirect:/booking/my";
    }

    /**
     * Người dùng xác nhận đã nhận được tiền hoàn trả
     */
    @PostMapping("/{id}/confirm-refund")
    public String confirmRefundReceived(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        boolean ok = bookingService.userConfirmRefundReceived(id, user);
        if (ok) {
            ra.addFlashAttribute("message", "Cảm ơn! Bạn đã xác nhận đã nhận tiền hoàn trả.");
        } else {
            ra.addFlashAttribute("error", "Không thể xác nhận nhận tiền (kiểm tra trạng thái hoàn tiền).");
        }
        return "redirect:/booking/my";
    }
}