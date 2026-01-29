package com.hotelbookingsystem.controller;

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
            Model model
    ) {
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

    @PostMapping("/confirm")
    public String confirmBooking(
            @RequestParam Long roomId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam int guests,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("user");
        Room room = roomService.getRoomById(roomId);

        bookingService.createBooking(user, room, checkIn, checkOut, guests);
        return "redirect:/booking/success";
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

        boolean success = bookingService.cancelBooking(id, user);

        if (success) {
            ra.addFlashAttribute("message", "Hủy booking thành công!");
        } else {
            ra.addFlashAttribute("error", "Không thể hủy booking (đã hủy hoặc không tồn tại)");
        }

        return "redirect:/booking/my";
    }
}