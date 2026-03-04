-- Thêm cột auth_provider vào bảng users
ALTER TABLE users ADD auth_provider VARCHAR(20) DEFAULT 'LOCAL';

-- Cập nhật tất cả user hiện tại
UPDATE users SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL;