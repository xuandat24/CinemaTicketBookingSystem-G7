package com.G7.CTBS.dto;


import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponseDTO {

    private Long couponId;
    private String code;
    private String discountType;
    private Double discountValue;
    private Integer maxUsage;
    private Double minOrderAmount;
    private LocalDate expiryDate;
    private Integer usedCount;
    private Boolean active;
    private String status; // ACTIVE / EXPIRED / UPCOMING / MAXED_OUT
    private LocalDate startDate;
    private String description;
}