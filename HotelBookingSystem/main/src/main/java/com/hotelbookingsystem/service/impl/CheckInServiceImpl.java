package com.hotelbookingsystem.service.impl;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.enums.BookingStatus;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.enums.CheckInStatus;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.hotelbookingsystem.service.CheckInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CheckInServiceImpl implements CheckInService {

    @Autowired
    private BookingRepository bookingRepository;

    // Thư mục lưu ảnh CCCD, có thể cấu hình trong application.properties
    @Value("${app.upload.cccd-dir:uploads/cccd}")
    private String cccdUploadDir;

    // Danh sách content type được phép
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png"
    );

    // Kích thước file tối đa: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * Lấy danh sách booking sắp tới (7 ngày)
     */
    @Override
    public List<Booking> getUpcomingBookings(User user) {
        LocalDate today = LocalDate.now();
        LocalDate weekFromNow = today.plusDays(7);

        return bookingRepository.findByUser(user).stream()
                .filter(b -> {
                    if (b.getStatus() != BookingStatus.CONFIRMED) {
                        return false;
                    }
                    if (b.getCheckInStatus() != CheckInStatus.PENDING) {
                        return false;
                    }
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
        if (booking.getStatus() != BookingStatus.CONFIRMED) return false;
        if (booking.getCheckInStatus() != CheckInStatus.PENDING) return false;
        if (booking.getCheckIn().isBefore(LocalDate.now())) return false;
        if (booking.getCheckIn().isAfter(LocalDate.now().plusDays(1))) return false;
        return true;
    }

    /**
     * Tạo QR code dưới dạng Base64 image
     */
    @Override
    public String generateQRCode(Booking booking) {
        try {
            String qrData = String.format("BOOKING_%d_USER_%d_CHECKIN_%s",
                    booking.getId(),
                    booking.getUser().getId(),
                    booking.getCheckIn());

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 300, 300);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

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
     * - Lưu ảnh CCCD (mặt trước + mặt sau)
     * - Tạo QR code
     * - Lưu vào database
     */
    @Override
    public Optional<Booking> performOnlineCheckIn(Long bookingId, User user, String citizenId,
                                                  String notes, MultipartFile frontImage,
                                                  MultipartFile backImage) {
        Optional<Booking> maybe = bookingRepository.findByIdAndUser(bookingId, user);

        if (maybe.isEmpty()) {
            return Optional.empty();
        }

        Booking booking = maybe.get();

        // Kiểm tra điều kiện check-in
        if (!isEligibleForCheckIn(booking)) {
            return Optional.empty();
        }

        // Xác nhận CCCD khớp
        if (!user.getCitizenId().equals(citizenId)) {
            return Optional.empty();
        }

        // Validate và lưu ảnh CCCD
        try {
            // Validate ảnh mặt trước
            if (frontImage == null || frontImage.isEmpty()) {
                return Optional.empty();
            }
            if (!isValidImage(frontImage)) {
                return Optional.empty();
            }

            // Validate ảnh mặt sau
            if (backImage == null || backImage.isEmpty()) {
                return Optional.empty();
            }
            if (!isValidImage(backImage)) {
                return Optional.empty();
            }

            // Lưu ảnh mặt trước
            String frontPath = saveImage(frontImage, bookingId, "front");

            // Lưu ảnh mặt sau
            String backPath = saveImage(backImage, bookingId, "back");

            // Cập nhật đường dẫn ảnh vào booking
            booking.setCitizenIdFrontImage(frontPath);
            booking.setCitizenIdBackImage(backPath);

        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
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
     * Validate file ảnh: kiểm tra content type và kích thước
     */
    private boolean isValidImage(MultipartFile file) {
        // Kiểm tra content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return false;
        }

        // Kiểm tra kích thước (max 5MB)
        if (file.getSize() > MAX_FILE_SIZE) {
            return false;
        }

        return true;
    }

    /**
     * Lưu file ảnh vào thư mục uploads/cccd/
     * Tên file: {bookingId}_{side}_{UUID}.{ext}
     */
    private String saveImage(MultipartFile file, Long bookingId, String side) throws IOException {
        // Tạo thư mục nếu chưa tồn tại
        Path uploadPath = Paths.get(cccdUploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Lấy extension từ tên file gốc
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Tạo tên file unique
        String filename = bookingId + "_" + side + "_" + UUID.randomUUID() + extension;

        // Lưu file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về đường dẫn tương đối (để hiển thị trên web)
        return "/uploads/cccd/" + filename;
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