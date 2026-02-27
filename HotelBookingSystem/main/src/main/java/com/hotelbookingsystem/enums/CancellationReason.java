package com.hotelbookingsystem.enums;

/**
 * 🆕 Enum định nghĩa 6 lý do hủy booking
 */
public enum CancellationReason {
    PERSONAL_REASON("Lý do cá nhân"),
    PLAN_CHANGED("Kế hoạch thay đổi"),
    FIND_BETTER_PRICE("Tìm được giá tốt hơn"),
    HEALTH_ISSUE("Vấn đề sức khỏe"),
    WORK_EMERGENCY("Công việc khẩn cấp"),
    NO_REASON("Không muốn nói");

    private final String label;

    CancellationReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}