package com.G7.CTBS.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {
    private String status;        // SUCCESS | FAILED | PENDING
    private String message;
    private Long bookingId;
    private String transactionId;
    private Long amount;
    private String paymentTime;
}