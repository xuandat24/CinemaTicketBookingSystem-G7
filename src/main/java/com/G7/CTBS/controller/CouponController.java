package com.G7.CTBS.controller;

import com.G7.CTBS.dto.CouponRequestDTO;
import com.G7.CTBS.dto.CouponResponseDTO;
import com.G7.CTBS.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

// AdminCouponController.java
@Controller
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ==================== LIST ====================
    @GetMapping
    public String listCoupons(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Boolean active,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {

        Page<CouponResponseDTO> couponPage = couponService.getAllCoupons(keyword, active, page, size);

        model.addAttribute("coupons", couponPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", couponPage.getTotalPages());
        model.addAttribute("totalItems", couponPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);

        // Stats cho header
        long activeCount  = couponPage.getContent().stream().filter(c -> "ACTIVE".equals(c.getStatus())).count();
        long expiredCount = couponPage.getContent().stream().filter(c -> "EXPIRED".equals(c.getStatus())).count();
        model.addAttribute("totalCoupons", couponPage.getTotalElements());
        model.addAttribute("activeCoupons", activeCount);
        model.addAttribute("expiredCoupons", expiredCount);

        return "admin/coupons";
    }

    // ==================== ADD FORM ====================
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("coupon", new CouponRequestDTO());
        model.addAttribute("isEdit", false);
        return "admin/coupons-form";
    }

    @PostMapping("/add")
    public String createCoupon(@Valid @ModelAttribute("coupon") CouponRequestDTO request,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/coupons-form";
        }
        try {
            couponService.createCoupon(request);
            redirectAttributes.addFlashAttribute("success", "Coupon created successfully!");
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            return "admin/coupons-form";
        }
        return "redirect:/admin/coupons";
    }

    // ==================== EDIT FORM ====================
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            CouponResponseDTO coupon = couponService.getCouponById(id);

            // Map DTO sang RequestDTO để fill vào form
            CouponRequestDTO form = new CouponRequestDTO();
            form.setCode(coupon.getCode());
            form.setDiscountType(coupon.getDiscountType());
            form.setDiscountValue(coupon.getDiscountValue());
            form.setMaxUsage(coupon.getMaxUsage());
            form.setMinOrderAmount(coupon.getMinOrderAmount());
            form.setExpiryDate(coupon.getExpiryDate());
            form.setActive(coupon.getActive());

            model.addAttribute("coupon", form);
            model.addAttribute("couponId", id);
            model.addAttribute("isEdit", true);
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/coupons";
        }
        return "admin/coupons-form";
    }

    @PostMapping("/edit/{id}")
    public String updateCoupon(@PathVariable Long id,
                               @Valid @ModelAttribute("coupon") CouponRequestDTO request,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("couponId", id);
            model.addAttribute("isEdit", true);
            return "admin/coupons-form";
        }
        try {
            couponService.updateCoupon(id, request);
            redirectAttributes.addFlashAttribute("success", "Coupon updated successfully!");
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("couponId", id);
            model.addAttribute("isEdit", true);
            return "admin/coupons-form";
        }
        return "redirect:/admin/coupons";
    }

    // ==================== DELETE ====================
    @GetMapping("/delete/{id}")
    public String deleteCoupon(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            couponService.deleteCoupon(id);
            redirectAttributes.addFlashAttribute("success", "Coupon deleted successfully!");
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    // ==================== TOGGLE ACTIVE ====================
    @PostMapping("/toggle/{id}")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            couponService.toggleActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Coupon status updated!");
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/coupons";
    }
}