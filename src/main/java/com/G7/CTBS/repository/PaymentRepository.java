package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Dùng khi VNPay return — tìm theo transactionRef
    Optional<Payment> findByTransactionRef(String transactionRef);

    // Dùng cho API /status/{bookingId}
    Optional<Payment> findByBooking_BookingId(Long bookingId);

    // Lấy tất cả payment của booking (user thanh toán lại nhiều lần)
    List<Payment> findAllByBooking_BookingIdOrderByCreatedAtDesc(Long bookingId);
}