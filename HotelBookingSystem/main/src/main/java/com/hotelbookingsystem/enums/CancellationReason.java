package com.hotelbookingsystem.enums;

/**
 * Enum định nghĩa các lý do hủy đặt phòng
 */
public enum CancellationReason {
    CHANGE_OF_PLANS("Thay đổi kế hoạch di chuyển"),
    FOUND_ALTERNATIVE("Đã tìm được phương án lưu trú phù hợp hơn"),
    HEALTH_ISSUE("Lý do sức khỏe"),
    WORK_COMMITMENT("Bận công việc đột xuất"),
    FINANCIAL_REASON("Điều chỉnh ngân sách cá nhân"),
    TRAVEL_RESTRICTIONS("Hạn chế về di chuyển hoặc visa"),
    BOOKING_ERROR("Đặt phòng nhầm thông tin"),
    PERSONAL_REASON("Lý do cá nhân"),
    OTHER("Lý do khác");

    private final String label;

    CancellationReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}