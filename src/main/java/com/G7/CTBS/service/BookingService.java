package com.G7.CTBS.service;

import com.G7.CTBS.dto.BookingDTO;
import com.G7.CTBS.dto.BookingRequestDTO;
import com.G7.CTBS.entity.Booking;
import com.G7.CTBS.entity.BookingSeat;
import com.G7.CTBS.entity.Seat;
import com.G7.CTBS.entity.Showtime;
import com.G7.CTBS.repository.BookingRepository;
import com.G7.CTBS.repository.BookingSeatRepository;
import com.G7.CTBS.repository.SeatRepository;
import com.G7.CTBS.repository.ShowtimeRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          SeatRepository seatRepository,
                          ShowtimeRepository showtimeRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Transactional
    public BookingDTO createBooking(BookingRequestDTO request) {
        if (request == null || request.getShowtimeId() == null || request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "showtimeId and seatIds are required");
        }

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Showtime not found"));

        Long roomId = showtime.getRoom() != null ? showtime.getRoom().getRoomId() : null;
        if (roomId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Showtime has no room");
        }

        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more seats not found");
        }

        for (Seat seat : seats) {
            if (seat.getRoom() == null || !roomId.equals(seat.getRoom().getRoomId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat does not belong to showtime room");
            }
        }

        Set<Long> alreadyBooked = new HashSet<>(bookingSeatRepository.findBookedSeatIdsByShowtimeId(request.getShowtimeId()));
        for (Long seatId : request.getSeatIds()) {
            if (alreadyBooked.contains(seatId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "One or more seats already booked");
            }
        }

        double basePrice = showtime.getBasePrice() != null ? showtime.getBasePrice() : 0.0;
        double totalPrice = seats.stream()
                .mapToDouble(seat -> basePrice * (seat.getPriceFactor() != null ? seat.getPriceFactor() : 1.0))
                .sum();

        Booking booking = new Booking();
        booking.setShowtime(showtime);
        booking.setStatus("CONFIRMED");
        booking.setCreateTime(LocalDateTime.now());
        booking.setBookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setFinalPrice(totalPrice);

        Booking savedBooking = bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = seats.stream()
                .map(seat -> {
                    BookingSeat bs = new BookingSeat();
                    bs.setBooking(savedBooking);
                    bs.setShowtime(showtime);
                    bs.setSeat(seat);
                    return bs;
                })
                .collect(Collectors.toList());

        bookingSeatRepository.saveAll(bookingSeats);

        return BookingDTO.builder()
                .bookingId(savedBooking.getBookingId())
                .showtimeId(showtime.getShowtimeId())
                .bookingCode(savedBooking.getBookingCode())
                .finalPrice(savedBooking.getFinalPrice())
                .status(savedBooking.getStatus())
                .createTime(savedBooking.getCreateTime())
                .bookedSeatIds(request.getSeatIds())
                .build();
    }
}
