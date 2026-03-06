package com.hotelbookingsystem.repository;

import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.RefundTransaction;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.enums.RefundTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {

    Optional<RefundTransaction> findByBooking(Booking booking);

    Optional<RefundTransaction> findByBookingId(Long bookingId);

    List<RefundTransaction> findByUser(User user);

    List<RefundTransaction> findByStatus(RefundTransactionStatus status);

    List<RefundTransaction> findByStatusIn(List<RefundTransactionStatus> statuses);
}