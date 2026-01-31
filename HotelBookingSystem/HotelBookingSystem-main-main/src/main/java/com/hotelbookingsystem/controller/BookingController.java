package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.CancelResult;
import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.User;
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

@Controller
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

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

        // CHECK: User must complete profile
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
     * THAY ĐỔI: Sau khi confirm, không redirect ngay mà trả về trang VietQR
     */
    @PostMapping("/confirm")
    public String confirmBooking(
            @RequestParam Long roomId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam int guests,
            @RequestParam BigDecimal totalPrice, // Lấy số tiền từ hidden input của bookingConfirm.html
            HttpSession session,
            Model model, // Thêm Model để truyền dữ liệu sang vietqr.html
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        // DOUBLE CHECK: Profile must be complete
        if (user == null || !user.isProfileComplete()) {
            ra.addFlashAttribute("error", "Vui lòng cập nhật hồ sơ trước khi đặt phòng!");
            return "redirect:/profile";
        }

        Room room = roomService.getRoomById(roomId);

        // Tạo booking trong DB (giữ nguyên logic cũ)
        bookingService.createBooking(user, room, checkIn, checkOut, guests);

        // TRUYỀN DỮ LIỆU SANG TRANG VIETQR
        model.addAttribute("room", room);
        model.addAttribute("totalPrice", totalPrice);

        // Trả về view vietqr.html thay vì redirect
        return "vietqr";
    }

    @GetMapping("/success")
    public String success() {
        return "bookingSuccess";
    }

    @GetMapping("/my")
    public String myBookings(HttpSession session, Model model, RedirectAttributes ra) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        model.addAttribute("bookings", bookingService.getBookingsByUser(user));
        return "bookingList";
    }


    @PostMapping("/{id}/cancel")
    public String cancelBooking(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        CancelResult result = bookingService.cancelBooking(id, user);

        switch (result) {
            case SUCCESS:
                ra.addFlashAttribute("message", "Hủy booking thành công! Yêu cầu hoàn tiền đã được gửi.");
                break;
            case NOT_FOUND:
                ra.addFlashAttribute("error", "Không tìm thấy booking.");
                break;
            case ALREADY_CANCELLED:
                ra.addFlashAttribute("error", "Booking đã được hủy trước đó.");
                break;
            case TOO_LATE:
                ra.addFlashAttribute("error", "Không thể hủy booking trong vòng 24 giờ trước ngày nhận phòng.");
                break;
            default:
                ra.addFlashAttribute("error", "Không thể hủy booking.");
        }

        return "redirect:/booking/my";
    }

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