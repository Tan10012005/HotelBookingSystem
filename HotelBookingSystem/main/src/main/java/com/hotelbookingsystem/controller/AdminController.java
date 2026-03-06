package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.enums.BookingStatus;
import com.hotelbookingsystem.enums.RoomChangeStatus;
import com.hotelbookingsystem.enums.RoomStatus;
import com.hotelbookingsystem.repository.*;
import com.hotelbookingsystem.service.BookingService;
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
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private RoomChangeRequestRepository roomChangeRequestRepository;

    @Autowired
    private RoomChangeService roomChangeService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private RoomTypeRepository roomTypeRepo;

    @Autowired
    private BookingService bookingService;

    // ===== DASHBOARD =====
    @GetMapping({"/dashboard", ""})
    public String dashboard(HttpSession session, Model model, RedirectAttributes ra) {
        Admin admin = (Admin) session.getAttribute("admin");

        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập với tài khoản admin");
            return "redirect:/login";
        }

        long totalUsers       = userRepo.count();
        long totalRooms       = roomRepo.count();
        long totalBookings    = bookingRepo.count();
        long pendingChanges   = roomChangeRequestRepository.findByStatus(RoomChangeStatus.PENDING).size();
        long pendingRefunds   = bookingService.getPendingRefundTransactions().size();

        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalPendingRoomChanges", pendingChanges);
        model.addAttribute("totalPendingRefunds", pendingRefunds);

        return "admin/dashboard";
    }

    // ===== LOGOUT =====
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("admin");
        return "redirect:/login";
    }

    /* ===== USER MANAGEMENT ===== */

    @GetMapping("/users")
    public String listUsers(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        List<User> users = userRepo.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/users/{id}/ban")
    public String banUser(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        Optional<User> maybe = userRepo.findById(id);
        if (maybe.isPresent()) {
            User u = maybe.get();
            u.setIsActive(false);
            userRepo.save(u);
            ra.addFlashAttribute("message", "Đã ban user " + u.getEmail());
        } else {
            ra.addFlashAttribute("error", "Người dùng không tồn tại");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/activate")
    public String activateUser(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        Optional<User> maybe = userRepo.findById(id);
        if (maybe.isPresent()) {
            User u = maybe.get();
            u.setIsActive(true);
            userRepo.save(u);
            ra.addFlashAttribute("message", "Đã kích hoạt user " + u.getEmail());
        } else {
            ra.addFlashAttribute("error", "Người dùng không tồn tại");
        }
        return "redirect:/admin/users";
    }

    /* ===== ROOM MANAGEMENT ===== */

    @GetMapping("/rooms")
    public String listRooms(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        List<Room> rooms = roomRepo.findAll();
        model.addAttribute("rooms", rooms);
        return "admin/rooms";
    }

    @GetMapping("/rooms/new")
    public String newRoomForm(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        model.addAttribute("room", new Room());
        model.addAttribute("types", roomTypeRepo.findAll());
        return "admin/roomForm";
    }

    @PostMapping("/rooms")
    public String createRoom(
            @RequestParam String roomNumber,
            @RequestParam Long roomTypeId,
            @RequestParam BigDecimal pricePerNight,
            HttpSession session,
            RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";

        RoomType rt = roomTypeRepo.findById(roomTypeId).orElse(null);
        if (rt == null) {
            ra.addFlashAttribute("error", "Loại phòng không hợp lệ");
            return "redirect:/admin/rooms";
        }

        Room r = Room.builder()
                .roomNumber(roomNumber)
                .roomType(rt)
                .pricePerNight(pricePerNight)
                .status(RoomStatus.AVAILABLE)
                .build();
        roomRepo.save(r);
        ra.addFlashAttribute("message", "Thêm phòng thành công");
        return "redirect:/admin/rooms";
    }

    @GetMapping("/rooms/{id}/edit")
    public String editRoomForm(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        Optional<Room> maybe = roomRepo.findById(id);
        if (maybe.isEmpty()) {
            ra.addFlashAttribute("error", "Phòng không tồn tại");
            return "redirect:/admin/rooms";
        }
        model.addAttribute("room", maybe.get());
        model.addAttribute("types", roomTypeRepo.findAll());
        return "admin/roomForm";
    }

    @PostMapping("/rooms/{id}/update")
    public String updateRoom(
            @PathVariable Long id,
            @RequestParam String roomNumber,
            @RequestParam Long roomTypeId,
            @RequestParam BigDecimal pricePerNight,
            @RequestParam(required = false) RoomStatus status,
            HttpSession session,
            RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        Optional<Room> maybe = roomRepo.findById(id);
        if (maybe.isEmpty()) {
            ra.addFlashAttribute("error", "Phòng không tồn tại");
            return "redirect:/admin/rooms";
        }
        Room r = maybe.get();
        RoomType rt = roomTypeRepo.findById(roomTypeId).orElse(null);
        if (rt == null) {
            ra.addFlashAttribute("error", "Loại phòng không hợp lệ");
            return "redirect:/admin/rooms";
        }
        r.setRoomNumber(roomNumber);
        r.setRoomType(rt);
        r.setPricePerNight(pricePerNight);
        if (status != null) r.setStatus(status);
        roomRepo.save(r);
        ra.addFlashAttribute("message", "Cập nhật phòng thành công");
        return "redirect:/admin/rooms";
    }

    @PostMapping("/rooms/{id}/delete")
    public String deleteRoom(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        if (!roomRepo.existsById(id)) {
            ra.addFlashAttribute("error", "Phòng không tồn tại");
            return "redirect:/admin/rooms";
        }
        roomRepo.deleteById(id);
        ra.addFlashAttribute("message", "Xóa phòng thành công");
        return "redirect:/admin/rooms";
    }

    /* ===== BOOKING MANAGEMENT ===== */

    @GetMapping("/bookings")
    public String listBookings(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        List<Booking> bookings = bookingRepo.findAll();
        model.addAttribute("bookings", bookings);
        return "admin/bookings";
    }

    @PostMapping("/bookings/{id}/accept")
    public String acceptBooking(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        Optional<Booking> maybe = bookingRepo.findById(id);
        if (maybe.isEmpty()) {
            ra.addFlashAttribute("error", "Booking không tồn tại");
            return "redirect:/admin/bookings";
        }
        Booking b = maybe.get();
        b.setStatus(BookingStatus.CONFIRMED);
        bookingRepo.save(b);
        ra.addFlashAttribute("message", "Đã xác nhận booking #" + id);
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/{id}/deny")
    public String denyBooking(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        Optional<Booking> maybe = bookingRepo.findById(id);
        if (maybe.isEmpty()) {
            ra.addFlashAttribute("error", "Booking không tồn tại");
            return "redirect:/admin/bookings";
        }
        Booking b = maybe.get();
        b.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(b);

        Room room = b.getRoom();
        if (room != null && room.getStatus() == RoomStatus.BOOKED) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepo.save(room);
        }

        ra.addFlashAttribute("message", "Đã từ chối booking #" + id);
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/{id}/refund-transferred")
    public String markRefundTransferred(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";

        boolean ok = bookingService.adminMarkRefundTransferred(id);
        if (ok) {
            ra.addFlashAttribute("message", "Đã đánh dấu: đã chuyển tiền cho booking #" + id);
        } else {
            ra.addFlashAttribute("error", "Không thể đánh dấu chuyển tiền (kiểm tra trạng thái booking).");
        }
        return "redirect:/admin/bookings";
    }

    /* ===== REFUND TRANSACTION MANAGEMENT ===== */

    /**
     * Trang quản lý giao dịch hoàn tiền
     */
    @GetMapping("/refunds")
    public String listRefundTransactions(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";

        List<RefundTransaction> allTransactions = bookingService.getAllRefundTransactions();
        List<RefundTransaction> pendingTransactions = bookingService.getPendingRefundTransactions();

        model.addAttribute("transactions", allTransactions);
        model.addAttribute("pendingCount", pendingTransactions.size());
        return "admin/refunds";
    }

    /**
     * Admin xử lý giao dịch hoàn tiền - chuyển khoản cho user
     */
    @PostMapping("/refunds/{id}/process")
    public String processRefundTransaction(
            @PathVariable Long id,
            @RequestParam(required = false) String adminNote,
            HttpSession session,
            RedirectAttributes ra
    ) {
        if (!isAdminSession(session, ra)) return "redirect:/login";

        boolean ok = bookingService.adminProcessRefundTransaction(id, adminNote);
        if (ok) {
            ra.addFlashAttribute("message", "✅ Đã xử lý hoàn tiền thành công cho giao dịch #" + id);
        } else {
            ra.addFlashAttribute("error", "Không thể xử lý giao dịch (kiểm tra trạng thái).");
        }
        return "redirect:/admin/refunds";
    }

    /* ===== helpers ===== */
    private boolean isAdminSession(HttpSession session, RedirectAttributes ra) {
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập với tài khoản admin");
            return false;
        }
        return true;
    }

    /* ===== ROOM CHANGE MANAGEMENT ===== */

    @GetMapping("/roomchanges")
    public String listRoomChanges(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        model.addAttribute("requests", roomChangeService.getAllRequests());
        return "admin/roomchanges";
    }

    @PostMapping("/roomchanges/{id}/approve")
    public String approveRoomChange(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        boolean ok = roomChangeService.approveRequest(id);
        if (ok) {
            ra.addFlashAttribute("message", "Đã duyệt yêu cầu đổi phòng #" + id + ". Booking đã được cập nhật!");
        } else {
            ra.addFlashAttribute("error", "Không thể duyệt. Phòng mới có thể đã bị đặt hoặc yêu cầu không còn ở trạng thái chờ.");
        }
        return "redirect:/admin/roomchanges";
    }

    @PostMapping("/roomchanges/{id}/reject")
    public String rejectRoomChange(
            @PathVariable Long id,
            @RequestParam(required = false) String adminNote,
            HttpSession session,
            RedirectAttributes ra) {
        if (!isAdminSession(session, ra)) return "redirect:/login";
        boolean ok = roomChangeService.rejectRequest(id, adminNote);
        if (ok) {
            ra.addFlashAttribute("message", "Đã từ chối yêu cầu đổi phòng #" + id);
        } else {
            ra.addFlashAttribute("error", "Không thể từ chối yêu cầu này.");
        }
        return "redirect:/admin/roomchanges";
    }
}