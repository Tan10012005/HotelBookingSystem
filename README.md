# 🏨 Hotel Booking System

Một hệ thống đặt phòng khách sạn hiện đại, được xây dựng bằng **Spring Boot** với giao diện tương tác thân thiện. Hệ thống cung cấp các tính năng đặt phòng, quản lý booking, quản trị hệ thống và hỗ trợ chatbot AI.

---

## 📋 Mục Lục

- [Tính Năng Chính](#-tính-năng-chính)
- [Công Nghệ Sử Dụng](#-công-nghệ-sử-dụng)
- [Sơ Đồ Database](#-sơ-đồ-database)
- [Workflow 1: Quy Trình Đặt Phòng](#-workflow-1-quy-trình-đặt-phòng)
- [Workflow 2: Quy Trình Hủy Booking](#-workflow-2-quy-trình-hủy-booking)
- [Hướng Dẫn Cài Đặt](#️-hướng-dẫn-cài-đặt)
---

## ✨ Tính Năng Chính

### 👤 Cho Người Dùng
- ✅ Đăng nhập/Đăng ký tài khoản với email
- ✅ Tìm kiếm và lọc phòng theo tiêu chí
- ✅ Đặt phòng với xác nhận chi tiết
- ✅ Thanh toán qua VietQR
- ✅ Xem lịch sử booking
- ✅ Hủy booking và nhận hoàn tiền theo chính sách
- ✅ Cập nhật hồ sơ (Họ tên, SĐT, CCCD)
- ✅ Chatbot AI hỗ trợ 24/7 💬

### 🔧 Cho Quản Trị Viên
- ✅ Dashboard thống kê tổng quan
- ✅ Quản lý người dùng (Ban/Unban)
- ✅ Quản lý phòng (CRUD)
- ✅ Quản lý loại phòng (Standard, Deluxe, Suite)
- ✅ Duyệt/Từ chối booking
- ✅ Quản lý hoàn tiền cho booking bị hủy

---

## 🛠 Công Nghệ Sử Dụng

| Lĩnh Vực | Công Nghệ |
|---------|-----------|
| Backend | Spring Boot 3.x, Spring Web, Spring Data JPA |
| Frontend | HTML5, CSS3, Thymeleaf, JavaScript |
| Database | MySQL 8.0+ / PostgreSQL |
| Build | Maven 3.8+ |
| JDK | Java 17+ |
| Authentication | Session-based (HttpSession) |
| Payment | VietQR Integration |
| AI | Chatbot Service Integration |
| ORM | Hibernate JPA |
| Validation | Jakarta Persistence Annotations |


---

## 🗄 Sơ Đồ Database

### 📌 Bảng chính

**users**
- id
- email
- password
- role
- full_name
- phone
- cccd
- status

**rooms**
- id
- room_number
- type_id
- price
- status
- description

**room_types**
- id
- name
- capacity
- description

**bookings**
- id
- user_id
- room_id
- check_in
- check_out
- total_price
- status
- created_at

**payments**
- id
- booking_id
- amount
- method
- status
- created_at

---

## 🔄 Workflow 1: Quy Trình Đặt Phòng


### 🧩 Mô Tả:
- Người dùng đăng nhập hoặc tạo tài khoản
- Tìm phòng theo ngày, loại phòng hoặc giá
- Kiểm tra thông tin trước khi xác nhận
- Thanh toán QR và lưu booking vào hệ thống

---

## 🔄 Workflow 2: Quy Trình Hủy Booking

### 🧩 Mô Tả:
- Người dùng vào lịch sử booking
- Chọn booking hợp lệ để hủy
- Hệ thống kiểm tra chính sách hoàn tiền
- Cập nhật trạng thái booking và payment

---

## 🔄 Workflow 3: Quy Trình Check-in Online Đăng nhập → Xem booking sắp tới → Check-in online → Xác nhận thông tin CCCD → Nhận QR check-in → Hoàn tất 

### 🧩 Mô tả: 
- User vào booking sắp đến ngày nhận phòng 
- Xác nhận lại thông tin cá nhân 
- Update CCCD (nếu cần) 
- Hệ thống generate QR code 
- Lúc tới khách sạn chỉ cần quét QR

## 🔄 Workflow 4: Quy Trình Đổi Phòng (Room Upgrade / Change Room)


### 🧩 Mô tả:

Đăng nhập
- Xem booking đang sắp tới
- Để đổi phòng, booking phải được xác nhận và ngày check-in phải còn ít nhất 1 ngày
- Yêu cầu đổi phòng (Nâng hạng phòng hoặc ngang cấp)
- Hệ thống kiểm tra phòng trống
- Tính chênh lệch giá
- Thanh toán phần chênh lệch (nếu có)
- Admin duyệt
- Cập nhật booking & room
- Hoàn tất



## ⚙️ Hướng Dẫn Cài Đặt

### 📌 Yêu Cầu
- Java 17+
- Maven 3.8+
- Microsoft SQL Server 2019+

### 🚀 Clone Project
```bash
git clone https://github.com/your-repo/hotel-booking-system.git
cd hotel-booking-system

Database:
    username: sa
    password: 12345
    
Base URL: http://localhost:8080
```
---

## 👥 Team Members

🅳 <b>Nguyễn Hữu Duy</b> — Team Leader<br>
🆃 <b>Nguyễn Quốc Nhật Tân</b> — Co-Leader<br>
🅿 <b>Phạm Mai Tâm Phúc</b> — Member<br>
🆃 <b>Nguyễn Sỹ Tuấn</b> — Member<br>
🅻 <b>Huỳnh Minh Lộc</b> — Member

---

