package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.enums.BookingStatus;
import com.hotelbookingsystem.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RevenueService {

    @Autowired
    private BookingRepository bookingRepo;

    // =========================================================
    //  HELPER: lấy booking hợp lệ
    // =========================================================
    private List<Booking> confirmedBookings() {
        return bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING_CONFIRM)
                .filter(b -> b.getCreatedAt() != null)
                .collect(Collectors.toList());
    }

    // =========================================================
    //  DAILY — 30 ngày gần nhất
    // =========================================================
    public Map<String, BigDecimal> getDailyRevenue(int days) {
        LocalDate endDate   = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<Booking> bookings = confirmedBookings().stream()
                .filter(b -> {
                    LocalDate created = b.getCreatedAt().toLocalDate();
                    return !created.isBefore(startDate) && !created.isAfter(endDate);
                }).collect(Collectors.toList());

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            result.put(String.format("%02d/%02d", date.getDayOfMonth(), date.getMonthValue()), BigDecimal.ZERO);
        }

        for (Booking b : bookings) {
            LocalDate created = b.getCreatedAt().toLocalDate();
            String key = String.format("%02d/%02d", created.getDayOfMonth(), created.getMonthValue());
            if (result.containsKey(key))
                result.put(key, result.get(key).add(b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO));
        }
        return result;
    }

    // =========================================================
    //  MONTHLY — theo năm
    // =========================================================
    public Map<String, BigDecimal> getMonthlyRevenue(int year) {
        List<Booking> bookings = confirmedBookings().stream()
                .filter(b -> b.getCreatedAt().getYear() == year)
                .collect(Collectors.toList());

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) result.put("Tháng " + m, BigDecimal.ZERO);

        for (Booking b : bookings) {
            String key = "Tháng " + b.getCreatedAt().getMonthValue();
            result.put(key, result.get(key).add(b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO));
        }
        return result;
    }

    // =========================================================
    //  QUARTERLY — theo năm
    // =========================================================
    public Map<String, BigDecimal> getQuarterlyRevenue(int year) {
        List<Booking> bookings = confirmedBookings().stream()
                .filter(b -> b.getCreatedAt().getYear() == year)
                .collect(Collectors.toList());

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("Quý 1 (T1-T3)", BigDecimal.ZERO);
        result.put("Quý 2 (T4-T6)", BigDecimal.ZERO);
        result.put("Quý 3 (T7-T9)", BigDecimal.ZERO);
        result.put("Quý 4 (T10-T12)", BigDecimal.ZERO);

        for (Booking b : bookings) {
            int m = b.getCreatedAt().getMonthValue();
            String key;
            if      (m <= 3)  key = "Quý 1 (T1-T3)";
            else if (m <= 6)  key = "Quý 2 (T4-T6)";
            else if (m <= 9)  key = "Quý 3 (T7-T9)";
            else              key = "Quý 4 (T10-T12)";
            result.put(key, result.get(key).add(b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO));
        }
        return result;
    }

    // =========================================================
    //  YEARLY — 5 năm gần nhất
    // =========================================================
    public Map<String, BigDecimal> getYearlyRevenue() {
        int currentYear = LocalDate.now().getYear();
        List<Booking> bookings = confirmedBookings().stream()
                .filter(b -> b.getCreatedAt().getYear() >= currentYear - 4)
                .collect(Collectors.toList());

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int y = currentYear - 4; y <= currentYear; y++) result.put(String.valueOf(y), BigDecimal.ZERO);

        for (Booking b : bookings) {
            String key = String.valueOf(b.getCreatedAt().getYear());
            if (result.containsKey(key))
                result.put(key, result.get(key).add(b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO));
        }
        return result;
    }

    // =========================================================
    //  DATE RANGE — doanh thu từ ngày X đến ngày Y
    // =========================================================
    public Map<String, BigDecimal> getRevenueByDateRange(LocalDate from, LocalDate to) {
        List<Booking> bookings = confirmedBookings().stream()
                .filter(b -> {
                    LocalDate d = b.getCreatedAt().toLocalDate();
                    return !d.isBefore(from) && !d.isAfter(to);
                }).collect(Collectors.toList());

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            result.put(String.format("%02d/%02d", cur.getDayOfMonth(), cur.getMonthValue()), BigDecimal.ZERO);
            cur = cur.plusDays(1);
        }

        for (Booking b : bookings) {
            LocalDate d = b.getCreatedAt().toLocalDate();
            String key = String.format("%02d/%02d", d.getDayOfMonth(), d.getMonthValue());
            if (result.containsKey(key))
                result.put(key, result.get(key).add(b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO));
        }
        return result;
    }

    /** Tổng doanh thu trong khoảng ngày */
    public BigDecimal getTotalByDateRange(LocalDate from, LocalDate to) {
        return confirmedBookings().stream()
                .filter(b -> {
                    LocalDate d = b.getCreatedAt().toLocalDate();
                    return !d.isBefore(from) && !d.isAfter(to);
                })
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Số booking trong khoảng ngày */
    public long getBookingCountByDateRange(LocalDate from, LocalDate to) {
        return confirmedBookings().stream()
                .filter(b -> {
                    LocalDate d = b.getCreatedAt().toLocalDate();
                    return !d.isBefore(from) && !d.isAfter(to);
                }).count();
    }

    // =========================================================
    //  ROOM RANKINGS
    // =========================================================

    /** Phòng được đặt nhiều nhất (top N) — trả về Map<roomNumber, bookingCount> */
    public Map<String, Long> getMostBookedRooms(int limit) {
        return bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING_CONFIRM)
                .filter(b -> b.getRoom() != null)
                .collect(Collectors.groupingBy(b -> b.getRoom().getRoomNumber(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Phòng được đặt ít nhất (bottom N) */
    public Map<String, Long> getLeastBookedRooms(int limit) {
        return bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING_CONFIRM)
                .filter(b -> b.getRoom() != null)
                .collect(Collectors.groupingBy(b -> b.getRoom().getRoomNumber(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    // =========================================================
    //  YoY COMPARISON (so sánh cùng kỳ)
    // =========================================================

    /**
     * So sánh doanh thu theo tháng: năm hiện tại vs năm trước
     * Trả về Map có keys: "labels", "currentValues", "prevValues"
     */
    public Map<String, Object> getYearlyComparison(int year) {
        Map<String, BigDecimal> current = getMonthlyRevenue(year);
        Map<String, BigDecimal> prev    = getMonthlyRevenue(year - 1);

        List<String> labels = new ArrayList<>(current.keySet());
        List<BigDecimal> currentVals = new ArrayList<>(current.values());
        List<BigDecimal> prevVals    = new ArrayList<>(prev.values());

        // Tính % thay đổi tổng năm
        BigDecimal totalCurrent = currentVals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrev    = prevVals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        double changePct = 0;
        if (totalPrev.compareTo(BigDecimal.ZERO) > 0) {
            changePct = totalCurrent.subtract(totalPrev)
                    .divide(totalPrev, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels",        labels);
        result.put("currentValues", currentVals);
        result.put("prevValues",    prevVals);
        result.put("totalCurrent",  totalCurrent);
        result.put("totalPrev",     totalPrev);
        result.put("changePct",     changePct);
        result.put("year",          year);
        result.put("prevYear",      year - 1);
        return result;
    }

    /**
     * So sánh doanh thu theo quý: năm hiện tại vs năm trước
     */
    public Map<String, Object> getQuarterlyComparison(int year) {
        Map<String, BigDecimal> current = getQuarterlyRevenue(year);
        Map<String, BigDecimal> prev    = getQuarterlyRevenue(year - 1);

        List<String> labels = new ArrayList<>(current.keySet());
        List<BigDecimal> currentVals = new ArrayList<>(current.values());
        List<BigDecimal> prevVals    = new ArrayList<>(prev.values());

        BigDecimal totalCurrent = currentVals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrev    = prevVals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        double changePct = 0;
        if (totalPrev.compareTo(BigDecimal.ZERO) > 0) {
            changePct = totalCurrent.subtract(totalPrev)
                    .divide(totalPrev, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels",        labels);
        result.put("currentValues", currentVals);
        result.put("prevValues",    prevVals);
        result.put("totalCurrent",  totalCurrent);
        result.put("totalPrev",     totalPrev);
        result.put("changePct",     changePct);
        result.put("year",          year);
        result.put("prevYear",      year - 1);
        return result;
    }

    // =========================================================
    //  SUMMARY STATS
    // =========================================================
    public BigDecimal getTotalRevenue() {
        return confirmedBookings().stream()
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getCurrentMonthRevenue() {
        LocalDate now = LocalDate.now();
        return confirmedBookings().stream()
                .filter(b -> b.getCreatedAt().getYear() == now.getYear()
                        && b.getCreatedAt().getMonthValue() == now.getMonthValue())
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTodayRevenue() {
        LocalDate today = LocalDate.now();
        return confirmedBookings().stream()
                .filter(b -> b.getCreatedAt().toLocalDate().equals(today))
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getTotalConfirmedBookings() {
        return bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();
    }

    /** Tổng doanh thu năm cụ thể */
    public BigDecimal getRevenueByYear(int year) {
        return confirmedBookings().stream()
                .filter(b -> b.getCreatedAt().getYear() == year)
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Tổng booking năm cụ thể */
    public long getBookingCountByYear(int year) {
        return confirmedBookings().stream()
                .filter(b -> b.getCreatedAt().getYear() == year)
                .count();
    }
}