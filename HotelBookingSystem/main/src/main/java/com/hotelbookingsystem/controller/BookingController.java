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
import com.hotelbookingsystem.service.UserService;
import com.hotelbookingsystem.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserService userService;  // ✅ Cần để refresh session sau khi trừ ví

    // =========================================================
    //  BƯỚC 1: Preview thông tin đặt phòng + hiển thị số dư ví
    // =========================================================
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
            ra.addFlashAttribute("error",
                    "Vui lòng cập nhật đầy đủ thông tin hồ sơ (Họ tên, SĐT, CCCD) trước khi đặt phòng!");
            ra.addFlashAttribute("redirectUrl", "/rooms");
            return "redirect:/profile";
        }

        Room room = roomService.getRoomById(roomId);

        long days = checkOut.toEpochDay() - checkIn.toEpochDay();
        BigDecimal totalPrice = room.getPricePerNight()
                .multiply(BigDecimal.valueOf(days));

        // Lấy số dư ví để hiển thị lựa chọn thanh toán
        BigDecimal walletBalance = walletService.getBalance(user);
        boolean walletSufficient = walletBalance.compareTo(totalPrice) >= 0;

        model.addAttribute("room",            room);
        model.addAttribute("checkIn",         checkIn);
        model.addAttribute("checkOut",        checkOut);
        model.addAttribute("guests",          guests);
        model.addAttribute("totalPrice",      totalPrice);
        model.addAttribute("walletBalance",   walletBalance);
        model.addAttribute("walletSufficient", walletSufficient);

        return "bookingConfirm";
    }

    // =========================================================
    //  BƯỚC 2: Xác nhận đặt phòng — chọn WALLET hoặc QR
    // =========================================================
    @PostMapping("/confirm")
    public String confirmBooking(
            @RequestParam Long roomId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam int guests,
            @RequestParam BigDecimal totalPrice,
            @RequestParam(defaultValue = "QR") String paymentMethod,
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

        if ("WALLET".equals(paymentMethod)) {
            // ── THANH TOÁN BẰNG VÍ ──
            BigDecimal currentBalance = walletService.getBalance(user);
            boolean ok = walletService.debit(user, totalPrice);

            if (!ok) {
                ra.addFlashAttribute("error",
                        "Số dư ví không đủ! Số dư hiện tại: "
                                + String.format("%,.0f", currentBalance.doubleValue())
                                + " VND. Vui lòng chọn thanh toán QR.");
                return "redirect:/rooms/" + roomId;
            }

            // Tạo booking sau khi đã trừ ví thành công
            bookingService.createBooking(user, room, checkIn, checkOut, guests);

            // Refresh user trong session để cập nhật số dư ví mới
            User refreshed = userService.getUserById(user.getId());
            session.setAttribute("user", refreshed);

            // Truyền data vào trang walletSuccess
            BigDecimal remaining = walletService.getBalance(refreshed);
            model.addAttribute("room",             room);
            model.addAttribute("paidAmount",       totalPrice);
            model.addAttribute("remainingBalance", remaining);

            return "walletSuccess";

        } else {
            // ── THANH TOÁN QR (giữ nguyên luồng cũ) ──
            bookingService.createBooking(user, room, checkIn, checkOut, guests);

            model.addAttribute("room",       room);
            model.addAttribute("totalPrice", totalPrice);

            return "vietqr";
        }
    }

    // =========================================================
    //  BƯỚC 3: Trang thành công (dùng cho QR và redirect chung)
    // =========================================================
    @GetMapping("/success")
    public String success() {
        return "bookingSuccess";
    }

    // =========================================================
    //  Danh sách booking của tôi
    // =========================================================
    @GetMapping("/my")
    public String myBookings(HttpSession session, Model model, RedirectAttributes ra) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        List<Booking> bookings = bookingService.getBookingsByUser(user);

        // Map bookingId → RoomChangeRequest đang PENDING (để hiển thị trạng thái trên UI)
        Map<Long, RoomChangeRequest> pendingChangeMap = new HashMap<>();
        roomChangeRequestRepository.findByUser(user).stream()
                .filter(r -> r.getStatus() == RoomChangeStatus.PENDING)
                .forEach(r -> pendingChangeMap.put(r.getBooking().getId(), r));

        // Lấy số dư ví để hiển thị trên trang
        BigDecimal walletBalance = walletService.getBalance(user);

        model.addAttribute("bookings",        bookings);
        model.addAttribute("pendingChangeMap", pendingChangeMap);
        model.addAttribute("walletBalance",   walletBalance);

        return "bookingList";
    }

    // =========================================================
    //  Hủy booking — tiền hoàn sẽ vào ví sau khi admin duyệt
    //  (không cần nhập thông tin ngân hàng nữa)
    // =========================================================
    @PostMapping("/{id}/cancel")
    public String cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            HttpSession session,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        // Parse lý do hủy
        CancellationReason cancellationReason = CancellationReason.OTHER;
        try {
            if (reason != null && !reason.isEmpty()) {
                cancellationReason = CancellationReason.valueOf(reason);
            }
        } catch (IllegalArgumentException ignored) {
            cancellationReason = CancellationReason.OTHER;
        }

        // Truyền chuỗi rỗng cho bank info vì tiền hoàn vào ví thay vì chuyển khoản
        CancelResult result = bookingService.cancelBooking(
                id, user, cancellationReason,
                "", "", ""
        );

        switch (result) {
            case SUCCESS:
                Optional<Booking> bookingOpt = bookingRepository.findByIdAndUser(id, user);
                if (bookingOpt.isPresent()) {
                    Booking b = bookingOpt.get();
                    Integer refundPercentage = b.getRefundPercentage();
                    BigDecimal refundAmount  = b.getRefundAmount();
                    String reasonText = b.getCancellationReason() != null
                            ? b.getCancellationReason().getLabel()
                            : "Không xác định";

                    if (refundPercentage != null && refundPercentage > 0) {
                        String message = String.format(
                                "✅ Hủy đặt phòng thành công!\n"
                                        + "Lý do: %s\n"
                                        + "Hoàn %d%% — %,.0f VND sẽ được cộng vào ví của bạn sau khi admin xử lý.",
                                reasonText,
                                refundPercentage,
                                refundAmount != null ? refundAmount.doubleValue() : 0
                        );
                        ra.addFlashAttribute("message", message);
                    } else {
                        ra.addFlashAttribute("message",
                                "✅ Hủy đặt phòng thành công!\n"
                                        + "Lý do: " + reasonText + "\n"
                                        + "Không hoàn tiền do hủy trong vòng 3 ngày trước ngày check-in.");
                    }
                } else {
                    ra.addFlashAttribute("message", "✅ Hủy đặt phòng thành công.");
                }
                break;

            case NOT_FOUND:
                ra.addFlashAttribute("error", "Không tìm thấy thông tin đặt phòng. Vui lòng thử lại.");
                break;

            case ALREADY_CANCELLED:
                ra.addFlashAttribute("error", "Đặt phòng này đã được hủy trước đó.");
                break;

            case TOO_LATE:
                ra.addFlashAttribute("error", "Không thể hủy, đã quá thời hạn cho phép.");
                break;

            default:
                ra.addFlashAttribute("error", "Không thể xử lý yêu cầu hủy. Vui lòng thử lại.");
        }

        return "redirect:/booking/my";
    }

    // =========================================================
    //  User xác nhận đã nhận tiền hoàn (legacy flow)
    // =========================================================
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
            ra.addFlashAttribute("message", "✅ Xác nhận nhận tiền hoàn thành công.");
        } else {
            ra.addFlashAttribute("error", "Không thể xác nhận. Vui lòng kiểm tra trạng thái hoàn tiền.");
        }
        return "redirect:/booking/my";
    }
}