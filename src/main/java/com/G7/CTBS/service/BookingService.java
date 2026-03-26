package com.G7.CTBS.service;

import com.G7.CTBS.dto.BookingRequestDTO;
import com.G7.CTBS.entity.*;
import com.G7.CTBS.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
public class BookingService {
    @Autowired private BookingRepository bookingRepo;
    @Autowired private BookingSeatRepository bookingSeatRepo;
    @Autowired private BookingComboRepository bookingComboRepo;
    @Autowired private ComboRepository comboRepo;
    @Autowired private SeatRepository seatRepo;
    @Autowired private ShowtimeRepository showtimeRepo;
    @Autowired private UserRepository userRepo;
    @Autowired
    private BookingRepository bookingRepository;

    public List<Booking> findByUser(User user) {
        // Gọi đến Repository để lấy danh sách booking theo User và sắp xếp mới nhất lên đầu
        return bookingRepository.findByUserOrderByCreateTimeDesc(user);
    }

    @Transactional
    public Long createBooking(BookingRequestDTO dto) {
        // 1. Lưu Booking
        Booking booking = new Booking();
        booking.setUser(userRepo.findById(dto.getUserId()).orElse(null));
        booking.setShowtime(showtimeRepo.findById(dto.getShowtimeId()).orElse(null));
        booking.setFinalPrice(dto.getFinalPrice()); // Khớp Entity
        booking.setCreateTime(LocalDateTime.now()); // Khớp Entity
        booking.setStatus("PENDING");
        booking.setBookingCode("BC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        booking = bookingRepo.save(booking);

        // 2. Lưu Chi tiết ghế
        if (dto.getSeatIds() != null) {
            for (Long sId : dto.getSeatIds()) {
                BookingSeat bs = new BookingSeat();
                bs.setBooking(booking);
                bs.setSeat(seatRepo.findById(sId).orElse(null));
                bs.setShowtime(booking.getShowtime());
                bookingSeatRepo.save(bs);
            }
        }

        // 3. Lưu Chi tiết Combo
        if (dto.getCombos() != null) {
            for (BookingRequestDTO.ComboSelection cs : dto.getCombos()) {
                BookingCombo bc = new BookingCombo();
                bc.setBooking(booking);
                bc.setCombo(comboRepo.findById(cs.getComboId()).orElse(null));
                bc.setQuantity(cs.getQuantity());
                bookingComboRepo.save(bc);
            }
        }

        return booking.getBookingId(); // Trả về bookingId theo Entity
    }
}