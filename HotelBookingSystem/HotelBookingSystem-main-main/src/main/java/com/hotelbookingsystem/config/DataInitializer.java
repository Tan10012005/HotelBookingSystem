package com.hotelbookingsystem.config;

import com.hotelbookingsystem.entity.*;
import com.hotelbookingsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private RoomTypeRepository roomTypeRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @Override
    public void run(String... args) {

        User testUser = userRepo.findByEmail("user@test.com");
        if (testUser == null) {
            testUser = userRepo.save(User.builder()
                    .email("user@test.com")
                    .password("123456")
                    .role("USER")
                    .build());
        }

        RoomType standard = roomTypeRepo.findAll().stream().findFirst()
                .orElseGet(() -> roomTypeRepo.save(
                        RoomType.builder()
                                .name("STANDARD")
                                .capacity(2)
                                .basePrice(new BigDecimal("500000"))
                                .build()
                ));

        RoomType deluxe = roomTypeRepo.findAll().stream()
                .filter(rt -> "DELUXE".equals(rt.getName()))
                .findFirst()
                .orElseGet(() -> roomTypeRepo.save(
                        RoomType.builder()
                                .name("DELUXE")
                                .capacity(3)
                                .basePrice(new BigDecimal("800000"))
                                .build()
                ));

        Room room101 = roomRepo.findAll().stream()
                .filter(r -> "101".equals(r.getRoomNumber()))
                .findFirst()
                .orElseGet(() -> roomRepo.save(Room.builder()
                        .roomNumber("101")
                        .roomType(standard)
                        .pricePerNight(new BigDecimal("500000"))
                        .status(RoomStatus.AVAILABLE)
                        .build()));

        Room room102 = roomRepo.findAll().stream()
                .filter(r -> "102".equals(r.getRoomNumber()))
                .findFirst()
                .orElseGet(() -> roomRepo.save(Room.builder()
                        .roomNumber("102")
                        .roomType(standard)
                        .pricePerNight(new BigDecimal("500000"))
                        .status(RoomStatus.BOOKED)
                        .build()));

        Room room201 = roomRepo.findAll().stream()
                .filter(r -> "201".equals(r.getRoomNumber()))
                .findFirst()
                .orElseGet(() -> roomRepo.save(Room.builder()
                        .roomNumber("201")
                        .roomType(deluxe)
                        .pricePerNight(new BigDecimal("800000"))
                        .status(RoomStatus.BOOKED)
                        .build()));

        Room room202 = roomRepo.findAll().stream()
                .filter(r -> "202".equals(r.getRoomNumber()))
                .findFirst()
                .orElseGet(() -> roomRepo.save(Room.builder()
                        .roomNumber("202")
                        .roomType(deluxe)
                        .pricePerNight(new BigDecimal("800000"))
                        .status(RoomStatus.AVAILABLE)
                        .build()));

        if (bookingRepo.findByUserId(testUser.getId()).stream()
                .noneMatch(b -> b.getRoom().getRoomNumber().equals("102"))) {

            LocalDate checkIn1 = LocalDate.now().plusDays(3);
            LocalDate checkOut1 = LocalDate.now().plusDays(5);
            long days1 = checkOut1.toEpochDay() - checkIn1.toEpochDay();

            bookingRepo.save(Booking.builder()
                    .user(testUser)
                    .room(room102)
                    .checkIn(checkIn1)
                    .checkOut(checkOut1)
                    .guests(2)
                    .totalPrice(room102.getPricePerNight().multiply(BigDecimal.valueOf(days1)))
                    .status(BookingStatus.CONFIRMED)
                    .build());
        }

        if (bookingRepo.findByUserId(testUser.getId()).stream()
                .noneMatch(b -> b.getRoom().getRoomNumber().equals("201"))) {

            LocalDate checkIn2 = LocalDate.now().plusDays(7);
            LocalDate checkOut2 = LocalDate.now().plusDays(10);
            long days2 = checkOut2.toEpochDay() - checkIn2.toEpochDay();

            bookingRepo.save(Booking.builder()
                    .user(testUser)
                    .room(room201)
                    .checkIn(checkIn2)
                    .checkOut(checkOut2)
                    .guests(3)
                    .totalPrice(room201.getPricePerNight().multiply(BigDecimal.valueOf(days2)))
                    .status(BookingStatus.CONFIRMED)
                    .build());
        }

        if (bookingRepo.findByUserId(testUser.getId()).stream()
                .noneMatch(b -> b.getRoom().getRoomNumber().equals("101")
                        && b.getStatus() == BookingStatus.CANCELLED)) {

            LocalDate checkIn3 = LocalDate.now().minusDays(5);
            LocalDate checkOut3 = LocalDate.now().minusDays(3);
            long days3 = checkOut3.toEpochDay() - checkIn3.toEpochDay();

            bookingRepo.save(Booking.builder()
                    .user(testUser)
                    .room(room101)
                    .checkIn(checkIn3)
                    .checkOut(checkOut3)
                    .guests(1)
                    .totalPrice(room101.getPricePerNight().multiply(BigDecimal.valueOf(days3)))
                    .status(BookingStatus.CANCELLED)
                    .build());
        }

        System.out.println("Data initialized successfully!");
        System.out.println("Test user: user@test.com / 123456");
        System.out.println("Total rooms: " + roomRepo.count());
        System.out.println("Total bookings: " + bookingRepo.count());
    }
}