package com.G7.CTBS.entity;

import jakarta.persistence.*;
import lombok.Data;


import java.time.LocalDateTime;

@Entity
@Data
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(unique = true, nullable = false)
    private String code;

    private String type; // "PERCENTAGE" hoặc "FIXED"
    private Double discountValue; // Ví dụ: 10 (10%) hoặc 50000 (50k)
    private Double maxDiscount; // Giới hạn tối đa nếu là loại PERCENTAGE
    private Double minOrderValue; // Giá trị đơn hàng tối thiểu để áp dụng
    private LocalDateTime expiryDate;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean active;
}
