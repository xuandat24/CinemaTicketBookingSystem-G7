package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingId")
    private Booking booking;

    private String transactionId;   // vnp_TransactionNo từ VNPay
    private String provider;        // VNPAY
    private Double amount;
    private String paymentMethod;   // VNPAY
    private String paymentStatus;   // PENDING | SUCCESS | FAILED
    private String transactionRef;  // vnp_TxnRef (bookingId_timestamp)
    private String responseCode;    // vnp_ResponseCode (00 = thành công)  ← thêm mới
    private LocalDateTime paymentTime;
    private LocalDateTime createdAt; // ← thêm mới

    @PrePersist
    public void prePersist() {
        this.createdAt     = LocalDateTime.now();
        this.paymentStatus = "PENDING";
        this.provider      = "VNPAY";
        this.paymentMethod = "VNPAY";
    }
}