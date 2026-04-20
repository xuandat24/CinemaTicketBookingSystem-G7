package com.G7.CTBS.controller;

import com.G7.CTBS.entity.Coupon;
import com.G7.CTBS.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/coupons")
public class PublicCouponController {

    @Autowired
    private CouponRepository couponRepository;

    @GetMapping("/check")
    public ResponseEntity<?> checkCoupon(@RequestParam String code, @RequestParam Double orderValue) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. Tìm mã giảm giá
            Coupon coupon = couponRepository.findByCode(code.trim().toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại!"));

            // 2. Kiểm tra các điều kiện (Ngày, Trạng thái, Lượt dùng)
            if (!Boolean.TRUE.equals(coupon.getActive()) ||
                    (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now()))) {
                throw new RuntimeException("Mã giảm giá đã hết hạn hoặc không khả dụng!");
            }
            if (coupon.getUsedCount() != null && coupon.getMaxUsage() != null &&
                    coupon.getUsedCount() >= coupon.getMaxUsage()) {
                throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng!");
            }

            // 3. Kiểm tra Min Order (Giá trị đơn hàng tối thiểu)
            if (coupon.getMinOrderAmount() != null && orderValue < coupon.getMinOrderAmount()) {
                throw new RuntimeException("Đơn hàng chưa đạt tối thiểu " +
                        String.format("%,.0f", coupon.getMinOrderAmount()) + "đ để dùng mã này!");
            }

            // 4. Tính toán số tiền được giảm để báo cho Frontend
            double discountAmount = 0.0;
            if ("PERCENTAGE".equals(coupon.getDiscountType())) {
                discountAmount = orderValue * (coupon.getDiscountValue() / 100);
            } else {
                discountAmount = coupon.getDiscountValue();
            }

            // Không giảm quá số tiền của đơn hàng
            discountAmount = Math.min(discountAmount, orderValue);

            response.put("success", true);
            response.put("discountAmount", discountAmount);
            response.put("message", "Áp dụng mã thành công!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}