package com.G7.CTBS.service;

import com.G7.CTBS.dto.SeatAvailabilityDTO;
import com.G7.CTBS.dto.ShowtimeSeatResponseDTO;
import com.G7.CTBS.entity.Seat;
import com.G7.CTBS.entity.Showtime;
import com.G7.CTBS.repository.BookingSeatRepository;
import com.G7.CTBS.repository.SeatRepository;
import com.G7.CTBS.repository.ShowtimeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SeatService {
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeRepository showtimeRepository;

    public SeatService(SeatRepository seatRepository,
                       BookingSeatRepository bookingSeatRepository,
                       ShowtimeRepository showtimeRepository) {
        this.seatRepository = seatRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    public ShowtimeSeatResponseDTO getSeatsForShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Showtime not found"));

        Long roomId = showtime.getRoom() != null ? showtime.getRoom().getRoomId() : null;
        List<Seat> seats = roomId == null ? List.of() : seatRepository.findByRoom_RoomId(roomId);
        Set<Long> bookedSeatIds = new HashSet<>(bookingSeatRepository.findBookedSeatIdsByShowtimeId(showtimeId));

        List<SeatAvailabilityDTO> seatDtos = seats.stream()
                .map(seat -> SeatAvailabilityDTO.builder()
                        .seatId(seat.getSeatId())
                        .seatCode(seat.getSeatCode())
                        .seatType(seat.getSeatType())
                        .priceFactor(seat.getPriceFactor())
                        .booked(bookedSeatIds.contains(seat.getSeatId()))
                        .build())
                .collect(Collectors.toList());

        return ShowtimeSeatResponseDTO.builder()
                .showtimeId(showtimeId)
                .basePrice(showtime.getBasePrice())
                .seats(seatDtos)
                .build();
    }
}
