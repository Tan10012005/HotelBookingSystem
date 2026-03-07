package com.hotelbookingsystem.service;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.enums.BookingStatus;
import com.hotelbookingsystem.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class RevenueService {

    @Autowired
    private BookingRepository bookingRepo;

    /**
     * Doanh thu theo từng ngày trong 30 ngày gần nhất
     */
    public Map<String, BigDecimal> getDailyRevenue(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<Booking> bookings = bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING_CONFIRM)
                .filter(b -> b.getCreatedAt() != null)
                .filter(b -> {
                    LocalDate created = b.getCreatedAt().toLocalDate();
                    return !created.isBefore(startDate) && !created.isAfter(endDate);
                })
                .toList();

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String key = String.format("%02d/%02d", date.getDayOfMonth(), date.getMonthValue());
            result.put(key, BigDecimal.ZERO);
        }

        for (Booking b : bookings) {
            LocalDate created = b.getCreatedAt().toLocalDate();
            String key = String.format("%02d/%02d", created.getDayOfMonth(), created.getMonthValue());
            if (result.containsKey(key)) {
                result.put(key, result.get(key).add(
                        b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO));
            }
        }

        return result;
    }

    /**
     * Doanh thu theo từng tháng trong năm được chọn
     */
    public Map<String, BigDecimal> getMonthlyRevenue(int year) {
        List<Booking> bookings = bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING_CONFIRM)
                .filter(b -> b.getCreatedAt() != null
                        && b.getCreatedAt().getYear() == year)
                .toList();

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            String key = "Tháng " + m;
            result.put(key, BigDecimal.ZERO);
        }

        for (Booking b : bookings) {
            int month = b.getCreatedAt().getMonthValue();
            String key = "Tháng " + month;
            result.put(key, result.get(key).add(
                    b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO));
        }

        return result;
    }

    /**
     * Doanh thu theo từng quý trong năm được chọn
     */
    public Map<String, BigDecimal> getQuarterlyRevenue(int year) {
        List<Booking> bookings = bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING_CONFIRM)
                .filter(b -> b.getCreatedAt() != null
                        && b.getCreatedAt().getYear() == year)
                .toList();

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("Quý 1 (T1-T3)", BigDecimal.ZERO);
        result.put("Quý 2 (T4-T6)", BigDecimal.ZERO);
        result.put("Quý 3 (T7-T9)", BigDecimal.ZERO);
        result.put("Quý 4 (T10-T12)", BigDecimal.ZERO);

        for (Booking b : bookings) {
            int month = b.getCreatedAt().getMonthValue();
            String key;
            if (month <= 3) key = "Quý 1 (T1-T3)";
            else if (month <= 6) key = "Quý 2 (T4-T6)";
            else if (month <= 9) key = "Quý 3 (T7-T9)";
            else key = "Quý 4 (T10-T12)";

            result.put(key, result.get(key).add(
                    b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO));
        }

        return result;
    }

    /**
     * Doanh thu theo từng năm (5 năm gần nhất)
     */
    public Map<String, BigDecimal> getYearlyRevenue() {
        int currentYear = LocalDate.now().getYear();

        List<Booking> bookings = bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING_CONFIRM)
                .filter(b -> b.getCreatedAt() != null
                        && b.getCreatedAt().getYear() >= currentYear - 4)
                .toList();

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int y = currentYear - 4; y <= currentYear; y++) {
            result.put(String.valueOf(y), BigDecimal.ZERO);
        }

        for (Booking b : bookings) {
            String key = String.valueOf(b.getCreatedAt().getYear());
            if (result.containsKey(key)) {
                result.put(key, result.get(key).add(
                        b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO));
            }
        }

        return result;
    }

    /**
     * Tổng doanh thu tất cả
     */
    public BigDecimal getTotalRevenue() {
        return bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING_CONFIRM)
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Doanh thu tháng hiện tại
     */
    public BigDecimal getCurrentMonthRevenue() {
        LocalDate now = LocalDate.now();
        return bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING_CONFIRM)
                .filter(b -> b.getCreatedAt() != null
                        && b.getCreatedAt().getYear() == now.getYear()
                        && b.getCreatedAt().getMonthValue() == now.getMonthValue())
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Doanh thu hôm nay
     */
    public BigDecimal getTodayRevenue() {
        LocalDate today = LocalDate.now();
        return bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING_CONFIRM)
                .filter(b -> b.getCreatedAt() != null
                        && b.getCreatedAt().toLocalDate().equals(today))
                .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Tổng số booking confirmed
     */
    public long getTotalConfirmedBookings() {
        return bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();
    }
}