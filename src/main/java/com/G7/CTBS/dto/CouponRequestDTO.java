package com.G7.CTBS.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

// CouponRequestDTO.java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequestDTO {

    private Long couponId;
    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 20, message = "Code must be 3–20 characters")
    private String code;

    @Enumerated(EnumType.STRING)
    @Pattern(regexp = "PERCENTAGE|FIXED", message = "Invalid discount type")
    private String discountType;

    @NotNull(message = "Discount value is required")
    @Min(value = 0, message = "Discount value must be >= 0")
    private Double discountValue;

    @Min(value = 0, message = "Max discount must be >= 0")
    private Integer maxUsage;

    @Min(value = 0, message = "Min order value must be >= 0")
    private Double minOrderAmount;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;


    private Boolean active = true;

    private LocalDate startDate;
    private String status;
}