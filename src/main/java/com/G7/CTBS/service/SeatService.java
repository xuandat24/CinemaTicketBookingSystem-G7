package com.G7.CTBS.service;

import com.G7.CTBS.dto.SeatAvailabilityResponseDTO;
import com.G7.CTBS.dto.SeatAvailabilitySeatDTO;
import com.G7.CTBS.entity.Movie;
import com.G7.CTBS.entity.Seat;
import com.G7.CTBS.entity.Showtime;
import com.G7.CTBS.entity.TheaterRoom;
import com.G7.CTBS.repository.BookingSeatRepository;
import com.G7.CTBS.repository.SeatRepository;
import com.G7.CTBS.repository.ShowtimeRepository;
import com.G7.CTBS.repository.TheaterRoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final ShowtimeRepository showtimeRepository;
    private final TheaterRoomRepository theaterRoomRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;

    @Transactional(readOnly = true)
    public SeatAvailabilityResponseDTO getSeatAvailabilityByShowtimeId(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));

        Long roomId = showtime.getTheaterRoom() != null ? showtime.getTheaterRoom().getRoomId() : null;
        if (roomId == null) {
            throw new EntityNotFoundException("Theater room not found");
        }

        TheaterRoom room = theaterRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Theater room not found"));

        Set<Long> bookedSeatIds = new HashSet<>(bookingSeatRepository.findBookedSeatIdsByShowtimeId(showtimeId));
        List<SeatAvailabilitySeatDTO> seatDTOs = seatRepository.findByRoom_RoomId(room.getRoomId())
                .stream()
                .sorted(Comparator.comparingInt(this::seatRowOrder)
                        .thenComparingInt(this::seatColumnOrder))
                .map(seat -> toSeatAvailabilityDTO(seat, bookedSeatIds.contains(seat.getSeatId())))
                .toList();

        Movie movie = showtime.getMovie();

        return SeatAvailabilityResponseDTO.builder()
                .showtimeId(showtime.getShowtimeId())
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .movieId(movie != null ? movie.getMovieId() : null)
                .movieTitle(movie != null ? movie.getTitle() : null)
                .bannerPath(movie != null ? movie.getBannerPath() : null)
                .duration(movie != null ? movie.getDuration() : null)
                .genre(extractGenre(movie))
                .startTime(showtime.getStartTime())
                .basePrice(showtime.getBasePrice())
                .seats(seatDTOs)
                .build();
    }

    private String extractGenre(Movie movie) {
        if (movie == null || movie.getCategories() == null || movie.getCategories().isEmpty()) {
            return null;
        }

        return movie.getCategories().stream()
                .map(category -> category != null ? category.getName() : null)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private SeatAvailabilitySeatDTO toSeatAvailabilityDTO(Seat seat, boolean isBooked) {
        return SeatAvailabilitySeatDTO.builder()
                .seatId(seat.getSeatId())
                .seatCode(seat.getSeatCode())
                .seatType(seat.getSeatType())
                .priceFactor(seat.getPriceFactor())
                .booked(isBooked)
                .build();
    }

    private int seatRowOrder(Seat seat) {
        String seatCode = safeSeatCode(seat.getSeatCode());
        if (seatCode.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        char row = seatCode.charAt(0);
        if (row >= 'A' && row <= 'Z') {
            return row - 'A';
        }
        return Integer.MAX_VALUE;
    }

    private int seatColumnOrder(Seat seat) {
        String seatCode = safeSeatCode(seat.getSeatCode());
        if (seatCode.length() == 1) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(seatCode.substring(1));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private String safeSeatCode(String seatCode) {
        return seatCode == null ? "" : seatCode.trim().toUpperCase(Locale.ROOT);
    }
}
