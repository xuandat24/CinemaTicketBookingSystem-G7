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
    
    private String transactionId;
    private String provider; // VD: VNPay, Momo
    private Double amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionRef;
    private LocalDateTime paymentTime;
}
