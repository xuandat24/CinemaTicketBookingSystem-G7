package com.G7.CTBS.service;

import com.G7.CTBS.dto.BookingRequestDTO;
import com.G7.CTBS.entity.*;
import com.G7.CTBS.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSeatRepository bookingSeatRepository;
    @Autowired private BookingComboRepository bookingComboRepository;
    @Autowired private ComboRepository comboRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CouponRepository couponRepository;

    public List<Booking> findByUser(User user) {
        return bookingRepository.findByUserOrderByCreateTimeDesc(user);
    }

    @Transactional
    public Long createBooking(BookingRequestDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Booking request is required.");

        Booking booking = new Booking();
        booking.setUser(userRepository.findById(dto.getUserId()).orElse(null));

        Showtime showtime = showtimeRepository.findById(dto.getShowtimeId())
                .orElseThrow(() -> new IllegalArgumentException("Showtime not found."));

        booking.setShowtime(showtime);
        booking.setCreateTime(LocalDateTime.now());
        booking.setStatus("PENDING");
        booking.setBookingCode("BC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // =========================================================================
        // 1. THUẬT TOÁN TÍNH GIÁ GỐC TRÊN BACKEND (BẢO MẬT CHỐNG HACK GIÁ)
        // =========================================================================
        double calculatedOriginalPrice = 0D;

        // Cộng tiền ghế
        if (dto.getSeatIds() != null) {
            for (Long seatId : dto.getSeatIds()) {
                if (seatId == null) continue;
                Seat seat = seatRepository.findById(seatId).orElse(null);
                if (seat != null && showtime.getBasePrice() != null) {
                    calculatedOriginalPrice += seat.getPriceFactor() * showtime.getBasePrice();
                }
            }
        }

        // Cộng tiền Combo
        if (dto.getCombos() != null) {
            for (BookingRequestDTO.ComboSelection comboSelection : dto.getCombos()) {
                if (comboSelection == null || comboSelection.getComboId() == null) continue;
                Combo combo = comboRepository.findById(comboSelection.getComboId()).orElse(null);
                if (combo != null) {
                    int quantity = comboSelection.getQuantity() == null ? 0 : comboSelection.getQuantity();
                    calculatedOriginalPrice += combo.getPrice() * quantity;
                }
            }
        }

        // =========================================================================
        // 2. XỬ LÝ MÃ GIẢM GIÁ (ÁP DỤNG TRÊN GIÁ GỐC VỪA TÍNH)
        // =========================================================================
        Double discountAmount = 0.0;
        Coupon usedCoupon = null;

        if (dto.getCouponCode() != null && !dto.getCouponCode().trim().isEmpty()) {
            usedCoupon = couponRepository.findByCode(dto.getCouponCode().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Coupon không tồn tại."));

            // Điều kiện 1: Check Active và Ngày hết hạn
            if (!Boolean.TRUE.equals(usedCoupon.getActive()) ||
                    (usedCoupon.getExpiryDate() != null && usedCoupon.getExpiryDate().isBefore(LocalDate.now()))) {
                throw new IllegalArgumentException("Coupon đã hết hạn hoặc không khả dụng.");
            }

            // Điều kiện 2: Check Giới hạn số người dùng chung
            if (usedCoupon.getMaxUsage() != null) {
                int currentUsage = usedCoupon.getUsedCount() == null ? 0 : usedCoupon.getUsedCount();
                if (currentUsage >= usedCoupon.getMaxUsage()) {
                    throw new IllegalArgumentException("Rất tiếc! Mã giảm giá này đã được sử dụng hết lượt.");
                }
            }

            // Điều kiện 3: Check Người dùng này đã dùng mã này chưa? (Mỗi người 1 lần)
            if (booking.getUser() != null) {
                // TẠO BIẾN TRUNG GIAN (Effectively Final) ĐỂ TRUYỀN VÀO LAMBDA
                String finalCouponCode = usedCoupon.getCode();

                boolean isAlreadyUsedByUser = bookingRepository.findByUserOrderByCreateTimeDesc(booking.getUser()).stream()
                        .anyMatch(b -> ("SUCCESS".equalsIgnoreCase(b.getStatus()) || "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                                && finalCouponCode.equalsIgnoreCase(b.getCouponCode())); // SỬ DỤNG BIẾN TRUNG GIAN Ở ĐÂY

                if (isAlreadyUsedByUser) {
                    throw new IllegalArgumentException("Bạn đã sử dụng mã giảm giá này trước đó rồi. Mỗi tài khoản chỉ được dùng 1 lần!");
                }
            }

            // Điều kiện 4: Check Min Order
            if (usedCoupon.getMinOrderAmount() != null && calculatedOriginalPrice < usedCoupon.getMinOrderAmount()) {
                throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu (" + usedCoupon.getMinOrderAmount() + "đ) để áp dụng mã này.");
            }

            // Tính tiền giảm (Code tính tiền của bạn giữ nguyên)
            if ("PERCENTAGE".equals(usedCoupon.getDiscountType())) {
                discountAmount = calculatedOriginalPrice * (usedCoupon.getDiscountValue() / 100.0); // Chú ý có .0 như đã vá ở lỗi trước
            } else if ("FIXED".equals(usedCoupon.getDiscountType())) {
                discountAmount = usedCoupon.getDiscountValue();
            }

            discountAmount = Math.min(discountAmount, calculatedOriginalPrice);

            // Cập nhật số lượt dùng
            usedCoupon.setUsedCount((usedCoupon.getUsedCount() == null ? 0 : usedCoupon.getUsedCount()) + 1);
            couponRepository.save(usedCoupon);
        }



        // Chốt giá cuối cùng
        double finalPrice = calculatedOriginalPrice - discountAmount;

        booking.setOriginalPrice(calculatedOriginalPrice);
        booking.setDiscountAmount(discountAmount);
        booking.setFinalPrice(finalPrice);

        // BỔ SUNG: Lưu vết Coupon để sau này Hủy đơn có thể hoàn lại
        if (usedCoupon != null) {
            booking.setCoupon(usedCoupon);
            booking.setCouponCode(usedCoupon.getCode());
        }


        // ... (code lưu BookingSeat, BookingCombo phía dưới) ...

        // Lưu Booking
        booking = bookingRepository.save(booking);

        // =========================================================================
        // 3. LƯU THÔNG TIN GHẾ VÀ COMBO VÀO BẢNG PHỤ
        // =========================================================================
        if (dto.getSeatIds() != null) {
            for (Long seatId : dto.getSeatIds()) {
                if (seatId == null) continue;
                Seat seat = seatRepository.findById(seatId).orElse(null);
                if (seat == null) continue;
                BookingSeat bookingSeat = new BookingSeat();
                bookingSeat.setBooking(booking);
                bookingSeat.setSeat(seat);
                bookingSeat.setShowtime(showtime);
                bookingSeatRepository.save(bookingSeat);
            }
        }

        if (dto.getCombos() != null) {
            for (BookingRequestDTO.ComboSelection comboSelection : dto.getCombos()) {
                if (comboSelection == null || comboSelection.getComboId() == null) continue;
                Combo combo = comboRepository.findById(comboSelection.getComboId()).orElse(null);
                if (combo == null) continue;
                int quantity = comboSelection.getQuantity() == null ? 0 : comboSelection.getQuantity();
                if (quantity <= 0) continue;

                BookingCombo bookingCombo = new BookingCombo();
                bookingCombo.setBooking(booking);
                bookingCombo.setCombo(combo);
                bookingCombo.setQuantity(quantity);
                bookingComboRepository.save(bookingCombo);
            }
        }

        return booking.getBookingId();
    }
}