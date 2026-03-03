package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.enums.CancelResult;
import com.hotelbookingsystem.enums.CancellationReason;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.service.BookingService;
import com.hotelbookingsystem.entity.RoomChangeRequest;
import com.hotelbookingsystem.enums.RoomChangeStatus;
import com.hotelbookingsystem.repository.RoomChangeRequestRepository;
import com.hotelbookingsystem.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
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
     * Bước 1: Xem trước thông tin đặt phòng (từ bookingForm.html chuyển sang)
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

        // KIỂM TRA: Người dùng phải hoàn tất hồ sơ mới được đặt phòng
        if (user == null || !user.isProfileComplete()) {
            ra.addFlashAttribute("error", "Vui lòng cập nhật đầy đủ thông tin hồ sơ (Họ tên, SĐT, CCCD) trước khi đặt phòng!");
            ra.addFlashAttribute("redirectUrl", "/rooms");
            return "redirect:/profile";
        }

        Room room = roomService.getRoomById(roomId);

        // Tính toán tổng tiền dựa trên số đêm lưu trú
        long days = checkOut.toEpochDay() - checkIn.toEpochDay();
        BigDecimal totalPrice = room.getPricePerNight()
                .multiply(BigDecimal.valueOf(days));

        model.addAttribute("room", room);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("guests", guests);
        model.addAttribute("totalPrice", totalPrice);

        return "bookingConfirm"; // Chuyển đến trang xác nhận thông tin đơn hàng
    }

    /**
     * Bước 2: Xác nhận đặt phòng và thực hiện thanh toán
     * Thay đổi: Sau khi bấm xác nhận, hệ thống lưu đơn vào DB và hiện trang VietQR thay vì Redirect thành công ngay.
     */
    @PostMapping("/confirm")
    public String confirmBooking(
            @RequestParam Long roomId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam int guests,
            @RequestParam BigDecimal totalPrice, // Nhận tổng tiền từ form để truyền sang trang thanh toán
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        // KIỂM TRA LẠI: Đảm bảo hồ sơ vẫn đầy đủ
        if (user == null || !user.isProfileComplete()) {
            ra.addFlashAttribute("error", "Vui lòng cập nhật hồ sơ trước khi đặt phòng!");
            return "redirect:/profile";
        }

        Room room = roomService.getRoomById(roomId);

        // Tạo đơn đặt phòng trong Database (Trạng thái mặc định là PENDING_CONFIRM)
        bookingService.createBooking(user, room, checkIn, checkOut, guests);

        // Nạp dữ liệu vào Model để trang vietqr.html có thông tin hiển thị mã QR động
        model.addAttribute("room", room);
        model.addAttribute("totalPrice", totalPrice);

        // Trả về View vietqr.html. Script trong trang này sẽ tự redirect sang /success sau khi "thanh toán".
        return "vietqr";
    }

    /**
     * Bước 3: Thông báo đặt phòng thành công (được gọi từ Javascript của vietqr.html)
     */
    @GetMapping("/success")
    public String success() {
        return "bookingSuccess";
    }

    /**
     * Xem danh sách các đơn đặt phòng của tôi
     */
    @GetMapping("/my")
    public String myBookings(HttpSession session, Model model, RedirectAttributes ra) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        List<Booking> bookings = bookingService.getBookingsByUser(user);

        // Lấy tất cả yêu cầu đổi phòng PENDING của user
        // Tạo Map<bookingId, RoomChangeRequest> để Thymeleaf tra cứu nhanh
        java.util.Map<Long, RoomChangeRequest> pendingChangeMap = new java.util.HashMap<>();
        roomChangeRequestRepository.findByUser(user).stream()
                .filter(r -> r.getStatus() == RoomChangeStatus.PENDING)
                .forEach(r -> pendingChangeMap.put(r.getBooking().getId(), r));

        model.addAttribute("bookings", bookings);
        model.addAttribute("pendingChangeMap", pendingChangeMap);
        return "bookingList";
    }

    /**
     * 🆕 Hủy đơn đặt phòng với logic hoàn tiền 50%/100%
     * Nhận lý do hủy từ form
     */
    @PostMapping("/{id}/cancel")
    public String cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,  // 🆕 NEW: reason parameter
            HttpSession session,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        // 🆕 NEW: Parse cancellation reason
        CancellationReason cancellationReason = CancellationReason.NO_REASON;
        try {
            if (reason != null && !reason.isEmpty()) {
                cancellationReason = CancellationReason.valueOf(reason);
            }
        } catch (IllegalArgumentException e) {
            cancellationReason = CancellationReason.NO_REASON;
        }

        // 🆕 UPDATED: Pass reason to service
        CancelResult result = bookingService.cancelBooking(id, user, cancellationReason);

        switch (result) {
            case SUCCESS:
                Optional<Booking> booking = bookingRepository.findByIdAndUser(id, user);
                if (booking.isPresent()) {
                    Booking b = booking.get();
                    Integer refundPercentage = b.getRefundPercentage();
                    BigDecimal refundAmount = b.getRefundAmount();
                    // 🆕 NEW: Include cancellation reason in message
                    String reasonText = b.getCancellationReason() != null
                            ? b.getCancellationReason().getLabel()
                            : "Không xác định";

                    String message = String.format(
                            "Hủy booking thành công!\n" +
                                    "Lý do: %s\n" +
                                    "Hoàn tiền: %d%% = %,.0f VND\n" +
                                    "Yêu cầu hoàn tiền đã được gửi.",
                            reasonText,
                            refundPercentage,
                            refundAmount.doubleValue()
                    );
                    ra.addFlashAttribute("message", message);
                } else {
                    ra.addFlashAttribute("message", "Hủy booking thành công! Yêu cầu hoàn tiền đã được gửi.");
                }
                break;
            case NOT_FOUND:
                ra.addFlashAttribute("error", "Không tìm thấy booking.");
                break;
            case ALREADY_CANCELLED:
                ra.addFlashAttribute("error", "Booking đã được hủy trước đó.");
                break;
            case TOO_LATE:
                ra.addFlashAttribute("error", "Không thể hủy booking. Đã quá hạn hủy.");
                break;
            default:
                ra.addFlashAttribute("error", "Không thể hủy booking.");
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