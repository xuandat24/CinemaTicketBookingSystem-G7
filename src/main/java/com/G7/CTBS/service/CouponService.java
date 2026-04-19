package com.G7.CTBS.service;

import com.G7.CTBS.dto.CouponRequestDTO;
import com.G7.CTBS.dto.CouponResponseDTO;
import com.G7.CTBS.entity.Coupon;
import org.springframework.data.domain.Page;

// CouponService.java
public interface CouponService {
    CouponResponseDTO createCoupon(CouponRequestDTO request);
    CouponResponseDTO getCouponById(Long id);
    CouponResponseDTO getCouponByCode(String code);
    Page<CouponResponseDTO> getAllCoupons(String keyword, Boolean active, int page, int size);
    CouponResponseDTO updateCoupon(Long id, CouponRequestDTO request);
    void deleteCoupon(Long id);
    void toggleActive(Long id);
    Coupon validateCoupon(String code, Double currentAmount);
    double calculateDiscount(Coupon coupon, Double totalAmount);
}