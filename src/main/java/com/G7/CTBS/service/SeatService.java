package com.G7.CTBS.service;

import com.G7.CTBS.dto.SeatAvailabilityResponseDTO;
import com.G7.CTBS.dto.SeatAvailabilitySeatDTO;
import com.G7.CTBS.entity.Seat;
import com.G7.CTBS.entity.Showtime; // Đã đổi sang dùng Showtime chính thức
import com.G7.CTBS.entity.TheaterRoom;
import com.G7.CTBS.repository.BookingSeatRepository;
import com.G7.CTBS.repository.SeatRepository;
import com.G7.CTBS.repository.ShowtimeRepository; // Đã đổi sang dùng ShowtimeRepository
import com.G7.CTBS.repository.TheaterRoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SeatService {

    // ĐÃ SỬA: Tiêm (Inject) ShowtimeRepository thay vì ShowtimesRecordRepository
    private final ShowtimeRepository showtimeRepository;
    private final TheaterRoomRepository theaterRoomRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;

    @Transactional(readOnly = true)
    public SeatAvailabilityResponseDTO getSeatAvailabilityByShowtimeId(Long showtimeId) {

        // ĐÃ SỬA: Tìm kiếm bằng Entity Showtime
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));

        // ĐÃ SỬA: Lấy ID phòng chiếu thông qua mối quan hệ TheaterRoom
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

        return SeatAvailabilityResponseDTO.builder()
                .showtimeId(showtime.getShowtimeId())
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                // ĐÃ SỬA: Lấy ID phim thông qua mối quan hệ Movie
                .movieId(showtime.getMovie() != null ? showtime.getMovie().getMovieId() : null)
                .startTime(showtime.getStartTime())
                .basePrice(showtime.getBasePrice())
                .seats(seatDTOs)
                .build();
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