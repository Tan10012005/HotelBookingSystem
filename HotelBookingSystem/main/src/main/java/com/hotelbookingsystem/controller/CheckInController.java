package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.service.CheckInService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller xử lý workflow check-in online
 */
@Controller
@RequestMapping("/checkin")
public class CheckInController {

    @Autowired
    private CheckInService checkInService;

    /**
     * BƯỚC 1: Hiển thị trang check-in online
     * Danh sách các booking sắp tới (7 ngày)
     */
    @GetMapping("/online")
    public String checkInPage(
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập trước!");
            return "redirect:/login";
        }

        // Lấy danh sách booking sắp tới
        List<Booking> upcomingBookings = checkInService.getUpcomingBookings(user);

        model.addAttribute("user", user);
        model.addAttribute("bookings", upcomingBookings);

        return "checkin/online";
    }

    /**
     * BƯỚC 2: Hiển thị form check-in cho booking cụ thể
     */
    @GetMapping("/form/{bookingId}")
    public String checkInForm(
            @PathVariable Long bookingId,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập trước!");
            return "redirect:/login";
        }

        // Lấy booking của user
        Optional<Booking> maybe = checkInService.getBookingByIdAndUser(bookingId, user);

        if (maybe.isEmpty()) {
            ra.addFlashAttribute("error", "Booking không tồn tại hoặc không phải của bạn!");
            return "redirect:/checkin/online";
        }

        Booking booking = maybe.get();

        // Kiểm tra điều kiện check-in
        if (!checkInService.isEligibleForCheckIn(booking)) {
            ra.addFlashAttribute("error", "Booking này không thể check-in lúc này!");
            return "redirect:/checkin/online";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("user", user);

        return "checkin/form";
    }

    /**
     * BƯỚC 3: Xử lý form check-in
     * - Xác nhận thông tin CCCD
     * - Tạo QR code
     * - Hiển thị QR code
     */
    @PostMapping("/confirm")
    public String confirmCheckIn(
            @RequestParam Long bookingId,
            @RequestParam String citizenId,
            @RequestParam(required = false) String notes,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập trước!");
            return "redirect:/login";
        }

        // Thực hiện check-in
        Optional<Booking> result = checkInService.performOnlineCheckIn(bookingId, user, citizenId, notes);

        if (result.isEmpty()) {
            ra.addFlashAttribute("error", "Xác nhận check-in thất bại! Vui lòng kiểm tra thông tin CCCD.");
            return "redirect:/checkin/form/" + bookingId;
        }

        Booking booking = result.get();

        // Chuyển đến trang hiển thị QR code
        return "redirect:/checkin/qr/" + booking.getId();
    }

    /**
     * BƯỚC 4: Hiển thị QR code
     */
    @GetMapping("/qr/{bookingId}")
    public String showQRCode(
            @PathVariable Long bookingId,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập trước!");
            return "redirect:/login";
        }

        Optional<Booking> maybe = checkInService.getBookingByIdAndUser(bookingId, user);

        if (maybe.isEmpty()) {
            ra.addFlashAttribute("error", "Booking không tồn tại!");
            return "redirect:/checkin/online";
        }

        Booking booking = maybe.get();

        if (booking.getQrCode() == null) {
            ra.addFlashAttribute("error", "QR code chưa được tạo!");
            return "redirect:/checkin/online";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("qrCode", booking.getQrCode());

        return "checkin/qr-display";
    }

    /**
     * API: Quét QR code tại cổng khách sạn
     * Cập nhật trạng thái check-in thực tế
     */
    @PostMapping("/api/scan-qr")
    @ResponseBody
    public Map<String, Object> scanQRCode(@RequestParam String qrCode) {
        Map<String, Object> response = new HashMap<>();

        try {
            checkInService.confirmCheckInByQR(qrCode);
            response.put("success", true);
            response.put("message", "Check-in thành công! Chào mừng tới khách sạn.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "QR code không hợp lệ!");
        }

        return response;
    }
}