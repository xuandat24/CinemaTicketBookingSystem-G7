package com.G7.CTBS.service;

import com.G7.CTBS.dto.BookingRequestDTO;
import com.G7.CTBS.entity.*;
import com.G7.CTBS.repository.BookingComboRepository;
import com.G7.CTBS.repository.BookingRepository;
import com.G7.CTBS.repository.BookingSeatRepository;
import com.G7.CTBS.repository.ComboRepository;
import com.G7.CTBS.repository.SeatRepository;
import com.G7.CTBS.repository.ShowtimeRepository;
import com.G7.CTBS.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private Double calculateDiscount(Coupon coupon, Double totalAmount) {
        if (coupon == null || !coupon.getActive() ||
                coupon.getExpiryDate().isBefore(LocalDateTime.now()) ||
                (coupon.getMinOrderValue() != null && totalAmount < coupon.getMinOrderValue())) {
            return 0D;
        }

        if ("PERCENTAGE".equalsIgnoreCase(coupon.getType())) {
            double discount = totalAmount * (coupon.getDiscountValue() / 100);
            // Nếu có quy định mức giảm tối đa (Max Discount)
            if (coupon.getMaxDiscount() != null && discount > coupon.getMaxDiscount()) {
                discount = coupon.getMaxDiscount();
            }
            return discount;
        } else if ("FIXED".equalsIgnoreCase(coupon.getType())) {
            return Math.min(coupon.getDiscountValue(), totalAmount);
        }

        return 0D;
    }
}