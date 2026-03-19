package com.G7.CTBS.service;

import com.G7.CTBS.dto.CreateShowtimeRequest;
import com.G7.CTBS.dto.ShowtimeResponse;
import com.G7.CTBS.dto.UpdateShowtimeRequest;
import com.G7.CTBS.entity.Movie;
import com.G7.CTBS.entity.Showtime;
import com.G7.CTBS.entity.TheaterRoom;
import com.G7.CTBS.exception.*;
import com.G7.CTBS.repository.BookingRepository;
import com.G7.CTBS.repository.MovieRepository;
import com.G7.CTBS.repository.ShowtimeRepository;
import com.G7.CTBS.repository.TheaterRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShowtimeServiceImpl implements ShowtimeService{
    private final MovieRepository movieRepository;
    private final TheaterRoomRepository theaterRoomRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;

    @Autowired
    public ShowtimeServiceImpl(
            MovieRepository movieRepository,
            TheaterRoomRepository theaterRoomRepository,
            ShowtimeRepository showtimeRepository,
            BookingRepository bookingRepository) {
        this.movieRepository = movieRepository;
        this.theaterRoomRepository = theaterRoomRepository;
        this.showtimeRepository = showtimeRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public ShowtimeResponse createShowtime(CreateShowtimeRequest request) {
        // 1. Check movie exists
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        // 2. Check theater room exists
        TheaterRoom room = theaterRoomRepository.findById(request.getTheaterRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Theater room not found"));

        // 3. Validate start time
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new InvalidShowtimeException("Showtime must be in the future");
        }

        // 4. Calculate end time (startTime + movie duration)
        LocalDateTime endTime = request.getStartTime().plusMinutes(movie.getDuration());

        // 5. Check schedule conflict in the same room
        boolean conflict = showtimeRepository
                .existsByTheaterRoomRoomIdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                        room.getRoomId(),
                        endTime,
                        request.getStartTime());

        if(conflict)
            throw new ShowtimeConflictException(
                    "Showtime overlaps with another show in this room");

        // 6. Create Showtime entity
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setTheaterRoom(room);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(endTime);
        showtime.setFormat(request.getFormat());
        showtime.setBasePrice(request.getPrice());

        // 7. Save to database
        Showtime savedShowtime = showtimeRepository.save(showtime);

        // 8. Response DTO
        return responseDTO(savedShowtime, movie, room);
    }

    @Override
    public ShowtimeResponse updateShowtime(Long showtimeId, UpdateShowtimeRequest request) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        boolean hasBooking =
                bookingRepository.existsByShowtimeShowtimeId(showtimeId);

        // ❌ nếu đã có booking → block toàn bộ
        if (hasBooking) {
            throw new ShowtimeHasBookingException(
                    "Cannot update showtime because bookings already exist");
        }

        Movie movie = showtime.getMovie();
        TheaterRoom room = showtime.getTheaterRoom();

        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new InvalidShowtimeException("Updated showtime must be in the future");
        }

        LocalDateTime newEndTime =
                request.getStartTime().plusMinutes(movie.getDuration());

        boolean conflict =
                showtimeRepository
                        .existsByTheaterRoomRoomIdAndShowtimeIdNotAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                                room.getRoomId(),
                                showtimeId,
                                newEndTime,
                                request.getStartTime());

        if (conflict) {
            throw new ShowtimeConflictException(
                    "Updated showtime conflicts with existing schedule");
        }

        // ✅ update full
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(newEndTime);
        showtime.setBasePrice(request.getPrice());
        showtime.setFormat(request.getFormat());

        Showtime updated = showtimeRepository.save(showtime);

        return responseDTO(updated, movie, room);
    }

    @Override
    public void deleteShowtime(Long showtimeId) {

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        // booking is not matching with this block code
//        boolean hasBooking =
//                bookingRepository.existsByShowtimeShowtimeId(showtimeId);
//
//        if(hasBooking){
//            throw new ShowtimeHasBookingException(
//                    "Cannot delete showtime because bookings already exist");
//        }

        showtimeRepository.delete(showtime);
    }

    @Override
    public ShowtimeResponse getShowtimeById(Long showtimeId) {

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        return responseDTO(
                showtime,
                showtime.getMovie(),
                showtime.getTheaterRoom());
    }

    @Override
    public List<ShowtimeResponse> getShowtimeByMovie(Long movieId) {

        movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        List<Showtime> showtime =
                showtimeRepository.findByMovieMovieId(movieId);

        return showtime.stream()
                .map(s -> responseDTO(s, s.getMovie(), s.getTheaterRoom()))
                .toList();
    }

    @Override
    public List<ShowtimeResponse> getShowtimeByRoom(Long roomId) {

        theaterRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        List<Showtime> showtime =
                showtimeRepository.findByTheaterRoomRoomId(roomId);

        return showtime.stream()
                .map(s -> responseDTO(s, s.getMovie(), s.getTheaterRoom()))
                .toList();
    }

    @Override
    public List<ShowtimeResponse> getShowtimeByDate(LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23,59,59);

        List<Showtime> showtime =
                showtimeRepository
                        .findByStartTimeBetween(startOfDay, endOfDay);

        return showtime.stream()
                .map(s -> responseDTO(s, s.getMovie(), s.getTheaterRoom()))
                .toList();
    }

    @Override
    public List<ShowtimeResponse> getAllShowtime() {

        List<Showtime> showtime = showtimeRepository.findAll();

        return showtime.stream()
                .map(s -> responseDTO(s, s.getMovie(), s.getTheaterRoom()))
                .toList();
    }

    // Convert to response DTO
    private ShowtimeResponse responseDTO(
            Showtime savedShowtime,
            Movie movie,
            TheaterRoom room) {
        ShowtimeResponse response = new ShowtimeResponse();
        response.setShowtimeId(savedShowtime.getShowtimeId());
        response.setMovieTitle(movie.getTitle());
        response.setTheaterRoomName(room.getRoomName());
        response.setStartTime(savedShowtime.getStartTime());
        response.setEndTime(savedShowtime.getEndTime());
        response.setFormat(savedShowtime.getFormat());
        response.setPrice(savedShowtime.getBasePrice());

        return response;
    }
}

