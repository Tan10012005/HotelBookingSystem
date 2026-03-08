package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.service.CheckInService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

        Optional<Booking> maybe = checkInService.getBookingByIdAndUser(bookingId, user);

        if (maybe.isEmpty()) {
            ra.addFlashAttribute("error", "Booking không tồn tại hoặc không phải của bạn!");
            return "redirect:/checkin/online";
        }

        Booking booking = maybe.get();

        if (!checkInService.isEligibleForCheckIn(booking)) {
            ra.addFlashAttribute("error", "Booking này không thể check-in lúc này!");
            return "redirect:/checkin/online";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("user", user);

        return "checkin/form";
    }

    /**
     * BƯỚC 3: Xử lý form check-in (có upload ảnh CCCD)
     */
    @PostMapping("/confirm")
    public String confirmCheckIn(
            @RequestParam Long bookingId,
            @RequestParam String citizenId,
            @RequestParam(required = false) String notes,
            @RequestParam("cccdFront") MultipartFile cccdFront,
            @RequestParam("cccdBack") MultipartFile cccdBack,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập trước!");
            return "redirect:/login";
        }

        // Validate ảnh CCCD trước khi gọi service
        if (cccdFront.isEmpty() || cccdBack.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng upload đầy đủ ảnh CCCD mặt trước và mặt sau!");
            return "redirect:/checkin/form/" + bookingId;
        }

        // Kiểm tra định dạng file
        String frontContentType = cccdFront.getContentType();
        String backContentType = cccdBack.getContentType();
        if (!isAllowedImageType(frontContentType) || !isAllowedImageType(backContentType)) {
            ra.addFlashAttribute("error", "Chỉ chấp nhận ảnh định dạng JPG, JPEG hoặc PNG!");
            return "redirect:/checkin/form/" + bookingId;
        }

        // Kiểm tra kích thước file (max 5MB)
        long maxSize = 5 * 1024 * 1024;
        if (cccdFront.getSize() > maxSize || cccdBack.getSize() > maxSize) {
            ra.addFlashAttribute("error", "Kích thước ảnh không được vượt quá 5MB!");
            return "redirect:/checkin/form/" + bookingId;
        }

        // Thực hiện check-in với ảnh CCCD
        Optional<Booking> result = checkInService.performOnlineCheckIn(
                bookingId, user, citizenId, notes, cccdFront, cccdBack
        );

        if (result.isEmpty()) {
            ra.addFlashAttribute("error", "Xác nhận check-in thất bại! Vui lòng kiểm tra thông tin CCCD.");
            return "redirect:/checkin/form/" + bookingId;
        }

        Booking booking = result.get();
        return "redirect:/checkin/qr/" + booking.getId();
    }

    /**
     * Kiểm tra content type có phải ảnh hợp lệ không
     */
    private boolean isAllowedImageType(String contentType) {
        if (contentType == null) return false;
        return contentType.equalsIgnoreCase("image/jpeg")
                || contentType.equalsIgnoreCase("image/jpg")
                || contentType.equalsIgnoreCase("image/png");
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