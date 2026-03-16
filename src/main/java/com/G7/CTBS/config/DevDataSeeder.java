package com.G7.CTBS.config;

import com.G7.CTBS.entity.Movie;
import com.G7.CTBS.entity.Seat;
import com.G7.CTBS.entity.Showtime;
import com.G7.CTBS.entity.TheaterRoom;
import com.G7.CTBS.repository.MovieRepository;
import com.G7.CTBS.repository.SeatRepository;
import com.G7.CTBS.repository.ShowtimeRepository;
import com.G7.CTBS.repository.TheaterRoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevDataSeeder implements CommandLineRunner {
    private final MovieRepository movieRepository;
    private final TheaterRoomRepository theaterRoomRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    public DevDataSeeder(MovieRepository movieRepository,
                         TheaterRoomRepository theaterRoomRepository,
                         SeatRepository seatRepository,
                         ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.theaterRoomRepository = theaterRoomRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public void run(String... args) {
        if (movieRepository.count() > 0 || theaterRoomRepository.count() > 0
                || seatRepository.count() > 0 || showtimeRepository.count() > 0) {
            return;
        }

        Movie movie = new Movie();
        movie.setTitle("Seed Movie");
        movie.setDescription("Seed movie for seat booking testing.");
        movie.setDuration(120);
        movie.setReleaseDate(LocalDate.now().minusDays(30));
        movie.setStatus("Now Playing");
        movie.setDirector("Seed Director");
        movie.setActors("Seed Actor");
        movie.setRating(7.5);
        Movie savedMovie = movieRepository.save(movie);

        TheaterRoom room = new TheaterRoom();
        room.setRoomName("Room 1");
        room.setTotalSeats(144);
        TheaterRoom savedRoom = theaterRoomRepository.save(room);

        List<Seat> seats = new ArrayList<>();
        String rows = "ABCDEFGHIJKL";
        for (int r = 0; r < rows.length(); r++) {
            String row = String.valueOf(rows.charAt(r));
            for (int c = 1; c <= 12; c++) {
                Seat seat = new Seat();
                seat.setRoom(savedRoom);
                seat.setSeatCode(row + c);
                seat.setSeatType(getSeatType(row));
                seat.setPriceFactor(getPriceFactor(row));
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);

        Showtime showtime = new Showtime();
        showtime.setMovie(savedMovie);
        showtime.setRoom(savedRoom);
        showtime.setStartTime(LocalDateTime.now().plusDays(1).withHour(19).withMinute(30).withSecond(0).withNano(0));
        showtime.setBasePrice(100000.0);
        showtimeRepository.save(showtime);
    }

    private String getSeatType(String row) {
        if ("L".equals(row)) {
            return "PAIR";
        }
        if ("DEFGHIJ".contains(row)) {
            return "VIP";
        }
        return "NORMAL";
    }

    private double getPriceFactor(String row) {
        if ("L".equals(row)) {
            return 1.25;
        }
        if ("DEFGHIJ".contains(row)) {
            return 1.5;
        }
        return 1.0;
    }
}
