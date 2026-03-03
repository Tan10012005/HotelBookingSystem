package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.repository.RoomRepository;
import com.hotelbookingsystem.service.RoomChangeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/roomchange")
public class RoomChangeController {

    @Autowired
    private RoomChangeService roomChangeService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    // ================================================================
    // BƯỚC 1: Chọn booking muốn đổi phòng
    // ================================================================
    @GetMapping("/select")
    public String selectBooking(HttpSession session, Model model, RedirectAttributes ra) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        List<Booking> eligibleBookings = roomChangeService.getEligibleBookingsForChange(user);
        model.addAttribute("bookings", eligibleBookings);
        model.addAttribute("user", user);
        return "roomchange/select";
    }

    // ================================================================
    // BƯỚC 2: Chọn phòng mới (danh sách phòng trống, cùng hoặc cao hơn)
    // ================================================================
    @GetMapping("/choose/{bookingId}")
    public String chooseRoom(
            @PathVariable Long bookingId,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        Optional<Booking> maybeBooking = bookingRepository.findByIdAndUser(bookingId, user);
        if (maybeBooking.isEmpty()) {
            ra.addFlashAttribute("error", "Booking không tồn tại hoặc không phải của bạn!");
            return "redirect:/roomchange/select";
        }

        Booking booking = maybeBooking.get();
        List<Room> availableRooms = roomChangeService.getAvailableRoomsForUpgrade(booking.getRoom());

        if (availableRooms.isEmpty()) {
            ra.addFlashAttribute("error", "Hiện không có phòng nào phù hợp để đổi. Vui lòng thử lại sau!");
            return "redirect:/roomchange/select";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("user", user);
        return "roomchange/choose";
    }

    // ================================================================
    // BƯỚC 3: Preview – tính chênh lệch giá trước khi xác nhận
    // ================================================================
    @PostMapping("/preview")
    public String previewChange(
            @RequestParam Long bookingId,
            @RequestParam Long newRoomId,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        Optional<Booking> maybeBooking = bookingRepository.findByIdAndUser(bookingId, user);
        Optional<Room> maybeRoom = roomRepository.findById(newRoomId);

        if (maybeBooking.isEmpty() || maybeRoom.isEmpty()) {
            ra.addFlashAttribute("error", "Thông tin không hợp lệ.");
            return "redirect:/roomchange/select";
        }

        Booking booking = maybeBooking.get();
        Room newRoom = maybeRoom.get();

        BigDecimal priceDifference = roomChangeService.calculatePriceDifference(booking, newRoom);
        long nights = booking.getCheckOut().toEpochDay() - booking.getCheckIn().toEpochDay();

        model.addAttribute("booking", booking);
        model.addAttribute("newRoom", newRoom);
        model.addAttribute("priceDifference", priceDifference);
        model.addAttribute("nights", nights);
        model.addAttribute("newTotalPrice", newRoom.getPricePerNight().multiply(BigDecimal.valueOf(nights)));
        model.addAttribute("user", user);
        return "roomchange/preview";
    }

    // ================================================================
    // BƯỚC 4: Xác nhận tạo yêu cầu đổi phòng
    // Nếu có chênh lệch giá → hiện trang thanh toán VietQR
    // Nếu ngang hạng (priceDiff = 0) → hiện success ngay
    // ================================================================
    @PostMapping("/request")
    public String submitRequest(
            @RequestParam Long bookingId,
            @RequestParam Long newRoomId,
            @RequestParam BigDecimal priceDifference,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        Optional<RoomChangeRequest> result = roomChangeService.createRequest(bookingId, newRoomId, user);

        if (result.isEmpty()) {
            ra.addFlashAttribute("error", "Không thể tạo yêu cầu đổi phòng. Vui lòng kiểm tra lại thông tin hoặc booking đã có yêu cầu đang chờ duyệt.");
            return "redirect:/roomchange/select";
        }

        RoomChangeRequest request = result.get();

        // Nếu có chênh lệch giá → hiện QR thanh toán
        if (priceDifference.compareTo(BigDecimal.ZERO) > 0) {
            Optional<Booking> maybeBooking = bookingRepository.findByIdAndUser(bookingId, user);
            if (maybeBooking.isPresent()) {
                model.addAttribute("booking", maybeBooking.get());
                model.addAttribute("request", request);
                model.addAttribute("priceDifference", priceDifference);
                model.addAttribute("newRoom", request.getNewRoom());
                return "roomchange/payment";
            }
        }

        // Ngang hạng → chuyển thẳng đến success
        ra.addFlashAttribute("requestId", request.getId());
        ra.addFlashAttribute("newRoom", request.getNewRoom());
        return "redirect:/roomchange/success";
    }

    // ================================================================
    // BƯỚC 5: Trang thành công (sau thanh toán hoặc ngang hạng)
    // ================================================================
    @GetMapping("/success")
    public String success(Model model) {
        return "roomchange/success";
    }

    // ================================================================
    // Danh sách tất cả yêu cầu đổi phòng của tôi
    // ================================================================
    @GetMapping("/my")
    public String myRequests(HttpSession session, Model model, RedirectAttributes ra) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        model.addAttribute("requests", roomChangeService.getRequestsByUser(user));
        model.addAttribute("user", user);
        return "roomchange/my";
    }
}