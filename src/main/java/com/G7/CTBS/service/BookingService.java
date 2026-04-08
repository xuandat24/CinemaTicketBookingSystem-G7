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
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private BookingComboRepository bookingComboRepository;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponRepository couponRepository;

    public List<Booking> findByUser(User user) {
        return bookingRepository.findByUserOrderByCreateTimeDesc(user);
    }

    @Transactional
    public Long createBooking(BookingRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Booking request is required.");
        }

        Booking booking = new Booking();
        booking.setUser(userRepository.findById(dto.getUserId()).orElse(null));

        Showtime showtime = showtimeRepository.findById(dto.getShowtimeId()).orElse(null);
        if (showtime == null) {
            throw new IllegalArgumentException("Showtime not found.");
        }

        booking.setShowtime(showtime);
        booking.setFinalPrice(dto.getFinalPrice() != null ? dto.getFinalPrice() : 0D);
        booking.setCreateTime(LocalDateTime.now());
        booking.setStatus("PENDING");
        booking.setBookingCode("BC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());


        // ==================== XỬ LÝ COUPON ====================
                // Lấy giá gốc từ DTO (FE gửi lên tổng tiền chưa giảm)
                Double originalPrice = dto.getFinalPrice() != null ? dto.getFinalPrice() : 0D;
        Double discountAmount = 0.0;
        Coupon usedCoupon = null;

        if (dto.getCouponCode() != null && !dto.getCouponCode().isEmpty()) {
            usedCoupon = couponRepository.findByCode(dto.getCouponCode())
                    .orElseThrow(() -> new IllegalArgumentException("Coupon không tồn tại."));

            // Kiểm tra Active và ExpiryDate (Entity của Huy đang dùng LocalDate)
            if (!Boolean.TRUE.equals(usedCoupon.getActive()) ||
                    (usedCoupon.getExpiryDate() != null && usedCoupon.getExpiryDate().isBefore(LocalDate.now()))) {
                throw new IllegalArgumentException("Coupon đã hết hạn hoặc không khả dụng.");
            }

            // Kiểm tra số lượt dùng
            if (usedCoupon.getUsedCount() != null && usedCoupon.getMaxUsage() != null &&
                    usedCoupon.getUsedCount() >= usedCoupon.getMaxUsage()) {
                throw new IllegalArgumentException("Coupon đã hết lượt sử dụng.");
            }

            // Kiểm tra giá tối thiểu
            if (usedCoupon.getMinOrderAmount() != null && originalPrice < usedCoupon.getMinOrderAmount()) {
                throw new IllegalArgumentException("Đơn hàng không đủ giá trị tối thiểu để dùng mã này.");
            }

            // Tính số tiền giảm (Khớp với các giá trị PERCENTAGE/FIXED ở Admin)
            if ("PERCENTAGE".equals(usedCoupon.getDiscountType())) {
                discountAmount = originalPrice * (usedCoupon.getDiscountValue() / 100);
            } else if ("FIXED".equals(usedCoupon.getDiscountType())) {
                discountAmount = usedCoupon.getDiscountValue();
            }

            discountAmount = Math.min(discountAmount, originalPrice);

            // Cập nhật lượt dùng
            usedCoupon.setUsedCount((usedCoupon.getUsedCount() == null ? 0 : usedCoupon.getUsedCount()) + 1);
            couponRepository.save(usedCoupon);
        }

        booking.setOriginalPrice(originalPrice);
        booking.setDiscountAmount(discountAmount);
        booking.setFinalPrice(originalPrice - discountAmount);

        // ======================================================

        booking = bookingRepository.save(booking);

        if (dto.getSeatIds() != null) {
            for (Long seatId : dto.getSeatIds()) {
                if (seatId == null) {
                    continue;
                }

                Seat seat = seatRepository.findById(seatId).orElse(null);
                if (seat == null) {
                    continue;
                }

                BookingSeat bookingSeat = new BookingSeat();
                bookingSeat.setBooking(booking);
                bookingSeat.setSeat(seat);
                bookingSeat.setShowtime(showtime);
                bookingSeatRepository.save(bookingSeat);
            }
        }

        if (dto.getCombos() != null) {
            for (BookingRequestDTO.ComboSelection comboSelection : dto.getCombos()) {
                if (comboSelection == null || comboSelection.getComboId() == null) {
                    continue;
                }

                Combo combo = comboRepository.findById(comboSelection.getComboId()).orElse(null);
                if (combo == null) {
                    continue;
                }

                int quantity = comboSelection.getQuantity() == null ? 0 : comboSelection.getQuantity();
                if (quantity <= 0) {
                    continue;
                }

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