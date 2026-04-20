package com.G7.CTBS.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {
    private Long paymentId;
    private Long bookingId;
    private String transactionId;
    private String provider;
    private Double amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionRef;
    private LocalDateTime paymentTime;
}