package com.G7.CTBS.service.impl;

import com.G7.CTBS.dto.CouponRequestDTO;
import com.G7.CTBS.dto.CouponResponseDTO;
import com.G7.CTBS.entity.Coupon;
import com.G7.CTBS.repository.CouponRepository;
import com.G7.CTBS.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

// CouponServiceImpl.java
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponRepository couponRepository;

    // ==================== CREATE ====================
    @Override
    @Transactional
    public CouponResponseDTO createCoupon(CouponRequestDTO request) {
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new IllegalArgumentException("Coupon code '" + request.getCode() + "' already exists");
        }

        Coupon coupon = new Coupon();
        mapRequestToEntity(request, coupon);
        coupon.setCode(request.getCode().toUpperCase());
        coupon.setUsedCount(0);

        return toResponseDTO(couponRepository.save(coupon));
    }

    // ==================== READ ====================
    @Override
    public CouponResponseDTO getCouponById(Long id) {
        Coupon coupon = findByIdOrThrow(id);
        return toResponseDTO(coupon);
    }

    @Override
    public CouponResponseDTO getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new NoSuchElementException("Coupon not found: " + code));
        return toResponseDTO(coupon);
    }

    @Override
    public Page<CouponResponseDTO> getAllCoupons(String keyword, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("couponId").descending());
        return couponRepository.searchCoupons(keyword, active, pageable)
                .map(this::toResponseDTO);
    }

    // ==================== UPDATE ====================
    @Override
    @Transactional
    public CouponResponseDTO updateCoupon(Long id, CouponRequestDTO request) {
        Coupon coupon = findByIdOrThrow(id);

        // Nếu đổi code thì kiểm tra trùng
        if (!coupon.getCode().equalsIgnoreCase(request.getCode()) &&
                couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new IllegalArgumentException("Coupon code '" + request.getCode() + "' already exists");
        }

        mapRequestToEntity(request, coupon);
        coupon.setCode(request.getCode().toUpperCase());

        return toResponseDTO(couponRepository.save(coupon));
    }

    // ==================== DELETE ====================
    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = findByIdOrThrow(id);
        couponRepository.delete(coupon);
    }

    // ==================== TOGGLE ====================
    @Override
    @Transactional
    public void toggleActive(Long id) {
        Coupon coupon = findByIdOrThrow(id);
        coupon.setActive(!coupon.getActive());
        couponRepository.save(coupon);
    }

    // ==================== HELPERS ====================
    private Coupon findByIdOrThrow(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coupon not found with id: " + id));
    }

    private void mapRequestToEntity(CouponRequestDTO request, Coupon coupon) {
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMaxUsage(request.getMaxUsage());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setActive(request.getActive() != null ? request.getActive() : true);
    }

    private CouponResponseDTO toResponseDTO(Coupon coupon) {
        String status = resolveStatus(coupon);
        return CouponResponseDTO.builder()
                .couponId(coupon.getCouponId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .maxUsage(coupon.getMaxUsage())
                .minOrderAmount(coupon.getMinOrderAmount())
                .expiryDate(coupon.getExpiryDate())
                .usedCount(coupon.getUsedCount())
                .active(coupon.getActive())
                .status(status)
                .build();
    }

    private String resolveStatus(Coupon coupon) {
        if (!coupon.getActive()) return "INACTIVE";
        if (LocalDate.now().isAfter(coupon.getExpiryDate())) return "EXPIRED";
        return "ACTIVE";
    }

    public Coupon validateCoupon(String code, Double currentAmount) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Coupon code does not exist."));

        // 1. Check active status
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new RuntimeException("This coupon is currently unavailable.");
        }

        // 2. Check expiry date
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("This coupon has expired.");
        }

        // 3. Check usage limit
        if (coupon.getMaxUsage() != null && coupon.getUsedCount() != null) {
            if (coupon.getUsedCount() >= coupon.getMaxUsage()) {
                throw new RuntimeException("This coupon has reached its usage limit.");
            }
        }

        // 4. Check minimum order amount
        if (coupon.getMinOrderAmount() != null && currentAmount < coupon.getMinOrderAmount()) {
            throw new RuntimeException(String.format(
                    "Order total must be at least %s VND to use this coupon.",
                    coupon.getMinOrderAmount()
            ));
        }

        return coupon;
    }

    /**
     * Tính toán số tiền được giảm
     */
    public double calculateDiscount(Coupon coupon, Double totalAmount) {
        double discount = 0;
        if ("PERCENTAGE".equals(coupon.getDiscountType())) {
            discount = totalAmount * (coupon.getDiscountValue() / 100);
        } else if ("FIXED".equals(coupon.getDiscountType())) {
            discount = coupon.getDiscountValue();
        }

        // Đảm bảo số tiền giảm không vượt quá tổng tiền hóa đơn
        return Math.min(discount, totalAmount);
    }
}