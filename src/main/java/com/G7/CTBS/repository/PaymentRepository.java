package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Latest payment row for a VNPay transaction ref.
    Optional<Payment> findFirstByTransactionRefOrderByCreatedAtDesc(String transactionRef);

    // Latest payment row for a booking.
    Optional<Payment> findFirstByBooking_BookingIdOrderByCreatedAtDesc(Long bookingId);

    // All payment rows for a booking (for cleanup/audit/retries).
    List<Payment> findAllByBooking_BookingIdOrderByCreatedAtDesc(Long bookingId);
}
