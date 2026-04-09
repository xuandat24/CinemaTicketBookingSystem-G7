package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(unique = true, nullable = false)
    private String code;

    private String discountType; // "PERCENTAGE" hoặc "FIXED"
    private Double discountValue; // Ví dụ: 10 (10%) hoặc 50000 (50k)
    private Integer maxUsage; // Giới hạn tối đa nếu là loại PERCENTAGE
    private Double minOrderAmount; // Giá trị đơn hàng tối thiểu để áp dụng
    private LocalDate expiryDate;
    private Integer usedCount;
    private Boolean active;
    private String description;
    private LocalDate startDate;
    private String status;
}
