package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.enums.BookingStatus;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.enums.CheckInStatus;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

@Service
public class CheckInServiceImpl implements CheckInService {

    @Autowired
    private BookingRepository bookingRepository;

    /**
     * Lấy danh sách booking sắp tới (7 ngày)
     */
    @Override
    public List<Booking> getUpcomingBookings(User user) {
        LocalDate today = LocalDate.now();
        LocalDate weekFromNow = today.plusDays(7);

        return bookingRepository.findByUser(user).stream()
                .filter(b -> {
                    // Chỉ lấy booking đã CONFIRMED và chưa check-in
                    if (b.getStatus() != BookingStatus.CONFIRMED) {
                        return false;
                    }
                    if (b.getCheckInStatus() != CheckInStatus.PENDING) {
                        return false;
                    }
                    // Chỉ lấy booking có check-in trong 7 ngày tới
                    return !b.getCheckIn().isBefore(today) &&
                            !b.getCheckIn().isAfter(weekFromNow);
                })
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra booking có thể check-in không
     */
    @Override
    public boolean isEligibleForCheckIn(Booking booking) {
        if (booking == null) return false;

        // Phải ở trạng thái CONFIRMED
        if (booking.getStatus() != BookingStatus.CONFIRMED) return false;

        // Phải chưa check-in
        if (booking.getCheckInStatus() != CheckInStatus.PENDING) return false;

        // Ngày check-in phải >= hôm nay
        if (booking.getCheckIn().isBefore(LocalDate.now())) return false;

        // Ngày check-in phải <= ngày hôm nay + 1 ngày (cho phép check-in sớm 1 ngày)
        if (booking.getCheckIn().isAfter(LocalDate.now().plusDays(1))) return false;

        return true;
    }

    /**
     * Tạo QR code dưới dạng Base64 image
     */
    @Override
    public String generateQRCode(Booking booking) {
        try {
            // Dữ liệu trong QR: bookingId + userId + checkInDate
            String qrData = String.format("BOOKING_%d_USER_%d_CHECKIN_%s",
                    booking.getId(),
                    booking.getUser().getId(),
                    booking.getCheckIn());

            // Tạo QR code (300x300 pixels)
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 300, 300);

            // Convert BitMatrix thành BufferedImage
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            // Convert thành Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageData = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageData);

            return "data:image/png;base64," + base64Image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Thực hiện check-in online:
     * - Xác nhận thông tin CCCD
     * - Tạo QR code
     * - Lưu vào database
     */
    @Override
    public Optional<Booking> performOnlineCheckIn(Long bookingId, User user, String citizenId, String notes) {
        Optional<Booking> maybe = bookingRepository.findByIdAndUser(bookingId, user);

        if (maybe.isEmpty()) {
            return Optional.empty();
        }

        Booking booking = maybe.get();

        // Kiểm tra điều kiện check-in
        if (!isEligibleForCheckIn(booking)) {
            return Optional.empty();
        }

        // Xác nhận CCCD khớp (so sánh với trường citizenId trong User)
        if (!user.getCitizenId().equals(citizenId)) {
            return Optional.empty(); // CCCD không khớp
        }

        // Tạo QR code
        String qrCode = generateQRCode(booking);
        if (qrCode == null) {
            return Optional.empty();
        }

        // Cập nhật booking
        booking.setQrCode(qrCode);
        booking.setCheckInStatus(CheckInStatus.CHECKED_IN);
        booking.setCheckInNotes(notes);
        booking.setStatus(BookingStatus.CONFIRMED);

        bookingRepository.save(booking);

        return Optional.of(booking);
    }

    /**
     * Lấy booking theo ID + User (kiểm tra quyền)
     */
    @Override
    public Optional<Booking> getBookingByIdAndUser(Long bookingId, User user) {
        return bookingRepository.findByIdAndUser(bookingId, user);
    }

    /**
     * Cập nhật trạng thái sau khi quét QR tại cổng
     */
    @Override
    public void confirmCheckInByQR(String qrCode) {
        Optional<Booking> maybe = bookingRepository.findByQrCode(qrCode);
        if (maybe.isPresent()) {
            Booking booking = maybe.get();
            booking.setActualCheckInTime(LocalDateTime.now());
            bookingRepository.save(booking);
        } else {
            throw new IllegalArgumentException("QR code không hợp lệ!");
        }
    }
}