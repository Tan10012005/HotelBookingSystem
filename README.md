# 🏨 Hotel Booking System

**Hệ thống đặt phòng khách sạn hiện đại**  
Xây dựng với Spring Boot (Java backend), giao diện HTML/CSS, tích hợp thanh toán VietQR và chatbot AI hỗ trợ 24/7.

---

## 📋 Mục Lục

- [Tính Năng Chính](#-tính-năng-chính)
- [Công Nghệ Sử Dụng](#-công-nghệ-sử-dụng)
- [Sơ Đồ Database](#-sơ-đồ-database)
- [Quy Trình Đặt Phòng](#-workflow-1-quy-trình-đặt-phòng)
- [Quy Trình Hủy Booking](#-workflow-2-quy-trình-hủy-booking)
- [Quy Trình Check-in Online](#-workflow-3-quy-trình-check-in-online)
- [Quy Trình Đổi Phòng](#-workflow-4-quy-trình-đổi-phòng)
- [Hướng Dẫn Cài Đặt & Sử Dụng](#️-hướng-dẫn-cài-đặt--sử-dụng)
- [Thông tin tác giả](#-thông-tin-tác-giả)

---

## ✨ Tính Năng Chính

### 👤 Cho Người Dùng
- Đăng nhập/Đăng ký tài khoản bằng email
- Tìm kiếm và lọc phòng theo loại, giá, ngày
- Đặt phòng khách sạn, xác nhận chi tiết
- Thanh toán bằng VietQR
- Xem lịch sử booking, quản lý booking cá nhân
- Hủy booking, nhận hoàn tiền theo chính sách
- Cập nhật hồ sơ cá nhân (Họ tên, SĐT, CCCD)
- Chatbot AI hỗ trợ thắc mắc 24/7
- Check-in online, nhận QR check-in

### 🔧 Cho Quản Trị Viên (Admin)
- Dashboard thống kê tổng quan
- Quản lý người dùng (Ban/Unban tài khoản)
- Quản lý phòng & loại phòng (Standard, Deluxe, Suite)
- Duyệt/từ chối booking, kiểm soát trạng thái phòng
- Quản lý hoàn tiền cho booking bị hủy
- Duyệt yêu cầu đổi phòng & nâng hạng

---

## 🛠 Công Nghệ Sử Dụng

- **HTML** (70.8%) - Giao diện, templates website
- **Java** (28.6%) - Backend, Spring Boot, xử lý logic, database
- **Other** (0.6%) - CSS, JS, Thư viện phụ trợ

Các thư viện chính:
- Spring Boot, Spring MVC, Spring Data JPA
- Thymeleaf Template Engine
- VietQR (Thanh toán QR)
- AI Chatbot (tích hợp support)

---

## 🗄 Sơ Đồ Database

### Bảng chính

**users**
- id, email, password, role, full_name, phone, cccd, status

**rooms**
- id, room_number, type_id, price, status, description

**room_types**
- id, name, capacity, description

**bookings**
- id, user_id, room_id, check_in, check_out, total_price, status, created_at

**payments**
- id, booking_id, amount, method, status, created_at

---

## 🔄 Workflow 1: Quy Trình Đặt Phòng

**Mô tả:**
- Người dùng đăng nhập hoặc tạo tài khoản
- Tìm phòng theo ngày, loại phòng hoặc giá
- Kiểm tra thông tin phòng, chọn số lượng khách, ngày nhận/trả phòng
- Thanh toán QR và lưu booking vào hệ thống
- Nhận thông báo xác nhận qua email

---

## 🔄 Workflow 2: Quy Trình Hủy Booking

**Mô tả:**
- Người dùng vào lịch sử booking
- Chọn booking hợp lệ để hủy
- Hệ thống kiểm tra chính sách hoàn tiền
- Cập nhật trạng thái booking và thanh toán

---

## 🔄 Workflow 3: Quy Trình Check-in Online

**Mô tả:**
- Đăng nhập hệ thống
- Xem booking sắp tới
- Tiến hành check-in online
- Xác nhận thông tin CCCD/hồ sơ
- Nhận QR check-in (dùng khi tới khách sạn)

---

## 🔄 Workflow 4: Quy Trình Đổi Phòng (Nâng hạng / Đổi phòng khác)

**Mô tả:**
- Đăng nhập, xem booking sắp tới
- Chỉ đổi phòng khi booking đã xác nhận, trước ngày check-in >= 1 ngày
- Yêu cầu đổi phòng (nâng hạng hoặc ngang cấp)
- Hệ thống kiểm tra phòng trống, tính chênh lệch giá
- Thanh toán phần chênh lệch (nếu có)
- Admin duyệt yêu cầu, cập nhật booking/phòng

---

## ⚙️ Hướng Dẫn Cài Đặt & Sử Dụng

### 1. Clone dự án
```bash
git clone https://github.com/Tan10012005/HotelBookingSystem.git
```
### 2. Yêu cầu môi trường
- Java JDK 17+
- Maven 3.6+
- MySQL 8+ hoặc H2 bản local

### 3. Cấu hình database
- Mở file `application.properties` và điền thông tin kết nối MySQL (hoặc H2)

### 4. Build & khởi chạy server
```bash
mvn clean install
mvn spring-boot:run
```
### 5. Truy cập ứng dụng
- **Frontend:** http://localhost:8080/
- Đăng ký tài khoản, sử dụng chức năng đặt phòng, thanh toán QR, quản lý booking.

### 6. Admin
- Truy cập bằng tài khoản vai trò ADMIN để quản lý phòng, booking, người dùng.

---

## 📄 License

Dự án mở nguồn phục vụ học tập và demo. Vui lòng ghi rõ nguồn khi sử dụng lại.
