-- Thêm cột cho check-in workflow vào bảng bookings

ALTER TABLE bookings ADD COLUMN qr_code LONGTEXT NULL;

ALTER TABLE bookings ADD COLUMN check_in_status VARCHAR(50) DEFAULT 'PENDING';

ALTER TABLE bookings ADD COLUMN actual_check_in_time DATETIME NULL;

ALTER TABLE bookings ADD COLUMN actual_check_out_time DATETIME NULL;

ALTER TABLE bookings ADD COLUMN check_in_notes TEXT NULL;

-- Tạo index cho tìm kiếm QR code
CREATE INDEX idx_qr_code ON bookings(qr_code(255));

-- Tạo index cho check-in status
CREATE INDEX idx_check_in_status ON bookings(check_in_status);