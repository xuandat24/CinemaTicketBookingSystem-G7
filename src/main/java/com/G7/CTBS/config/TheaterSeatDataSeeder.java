package com.G7.CTBS.config;

import com.G7.CTBS.entity.Seat;
import com.G7.CTBS.entity.TheaterRoom;
import com.G7.CTBS.repository.SeatRepository;
import com.G7.CTBS.repository.TheaterRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TheaterSeatDataSeeder implements CommandLineRunner {

    private static final int ROWS = 12;
    private static final int COLS = 12;
    private static final int REQUIRED_ROOMS = 10;

    private final TheaterRoomRepository theaterRoomRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<TheaterRoom> rooms = ensureRequiredTheaterRooms();
        for (TheaterRoom room : rooms) {
            ensureSeatLayout(room);
        }
    }

    private List<TheaterRoom> ensureRequiredTheaterRooms() {
        List<TheaterRoom> rooms = theaterRoomRepository.findAll(Sort.by(Sort.Direction.ASC, "roomId"));

        int missing = REQUIRED_ROOMS - rooms.size();
        for (int i = 0; i < missing; i++) {
            int roomNumber = rooms.size() + 1;
            TheaterRoom room = new TheaterRoom();
            room.setRoomName("Theater Room " + roomNumber);
            room.setTotalSeats(ROWS * COLS);
            rooms.add(theaterRoomRepository.save(room));
        }

        return rooms.subList(0, Math.min(REQUIRED_ROOMS, rooms.size()));
    }

    private void ensureSeatLayout(TheaterRoom room) {
        long seatCount = seatRepository.countByRoom_RoomId(room.getRoomId());
        if (seatCount >= ROWS * COLS) {
            if (room.getTotalSeats() == null || room.getTotalSeats() != ROWS * COLS) {
                room.setTotalSeats(ROWS * COLS);
                theaterRoomRepository.save(room);
            }
            return;
        }

        List<Seat> toInsert = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < ROWS; rowIndex++) {
            char rowLetter = (char) ('A' + rowIndex);
            for (int col = 1; col <= COLS; col++) {
                String seatCode = rowLetter + String.valueOf(col);
                if (seatRepository.existsByRoom_RoomIdAndSeatCode(room.getRoomId(), seatCode)) {
                    continue;
                }

                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setSeatCode(seatCode);
                seat.setSeatType(resolveSeatType(rowLetter));
                seat.setPriceFactor(resolvePriceFactor(rowLetter));
                toInsert.add(seat);
            }
        }

        if (!toInsert.isEmpty()) {
            seatRepository.saveAll(toInsert);
        }

        if (room.getTotalSeats() == null || room.getTotalSeats() != ROWS * COLS) {
            room.setTotalSeats(ROWS * COLS);
            theaterRoomRepository.save(room);
        }
    }

    private String resolveSeatType(char rowLetter) {
        if (rowLetter == 'L') {
            return "PAIR";
        }
        if (rowLetter >= 'D' && rowLetter <= 'J') {
            return "VIP";
        }
        return "NORMAL";
    }

    private double resolvePriceFactor(char rowLetter) {
        if (rowLetter == 'L') {
            return 1.25;
        }
        if (rowLetter >= 'D' && rowLetter <= 'J') {
            return 1.5;
        }
        return 1.0;
    }
}