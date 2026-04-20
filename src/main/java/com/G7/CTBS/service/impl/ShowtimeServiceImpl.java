package com.G7.CTBS.service.impl;

import com.G7.CTBS.dto.CreateShowtimeRequest;
import com.G7.CTBS.dto.ShowtimeResponse;
import com.G7.CTBS.dto.UpdateShowtimeRequest;
import com.G7.CTBS.entity.Movie;
import com.G7.CTBS.entity.Showtime;
import com.G7.CTBS.entity.TheaterRoom;
import com.G7.CTBS.enums.ShowtimeStatus;
import com.G7.CTBS.exception.*;
import com.G7.CTBS.repository.BookingRepository;
import com.G7.CTBS.repository.MovieRepository;
import com.G7.CTBS.repository.ShowtimeRepository;
import com.G7.CTBS.repository.TheaterRoomRepository;
import com.G7.CTBS.service.ShowtimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShowtimeServiceImpl implements ShowtimeService {
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

        // 3. VÁ LỖI THỜI GIAN: Tính chính xác Giờ kết thúc = Giờ bắt đầu + Thời lượng phim + 15p dọn dẹp
        LocalDateTime endTime = request.getStartTime().plusMinutes(movie.getDuration()).plusMinutes(15);

        // 4. Check schedule conflict in the same room
        boolean conflict = showtimeRepository
                .existsByTheaterRoomRoomIdAndStatusAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                        room.getRoomId(),
                        ShowtimeStatus.ACTIVE,
                        endTime,
                        request.getStartTime());

        if(conflict)
            throw new ShowtimeConflictException("The screening room is busy at this time (including 15 minutes for room cleaning). Please choose a different time!");

        // 5. Create Showtime entity
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setTheaterRoom(room);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(endTime);
        showtime.setFormat(request.getFormat());
        showtime.setBasePrice(request.getPrice());

        // 6. VÁ LỖI NGHIÊM TRỌNG: Gán trạng thái ACTIVE để thuật toán Check trùng lịch nhận diện được!
        showtime.setStatus(ShowtimeStatus.ACTIVE);

        // 7. Save to database
        Showtime savedShowtime = showtimeRepository.save(showtime);

        if (movie.getStatus().equalsIgnoreCase("Coming Soon")) {
            movie.setStatus("Now Playing");
            movieRepository.save(movie);
        }

        // 8. Response DTO
        return responseDTO(savedShowtime, movie, room);
    }

    @Override
    public ShowtimeResponse updateShowtime(Long showtimeId, UpdateShowtimeRequest request) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        boolean hasBooking =
                bookingRepository.existsByShowtimeShowtimeId(showtimeId);

        // =========================================================
        // CHỐT CHẶN 1: Không cho phép sửa suất chiếu đã diễn ra trong quá khứ
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Error: Unable to edit the start or end of a showtime!");
        }

        // CHỐT CHẶN 2: Không cho phép dời lịch mới về thời điểm trong quá khứ
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Error: The new showtime cannot be in the past!");
        }
        // =========================================================

        // If it has booking → block all
        if (hasBooking) {
            throw new ShowtimeHasBookingException(
                    "Unable to update showtimes because tickets have already been booked!");
        }
        // If it has booking → block all
        if (hasBooking) {
            throw new ShowtimeHasBookingException(
                    "Cannot update showtime because bookings already exist");
        }

        Movie movie = showtime.getMovie();
        TheaterRoom room = showtime.getTheaterRoom();

        // VÁ LỖI THỜI GIAN UPDATE: Bắt buộc cộng thêm 15p dọn phòng
        LocalDateTime newEndTime = request.getStartTime().plusMinutes(movie.getDuration()).plusMinutes(15);

        boolean conflict =
                showtimeRepository
                        .existsByTheaterRoomRoomIdAndShowtimeIdNotAndStatusAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                                room.getRoomId(),
                                showtimeId,
                                ShowtimeStatus.ACTIVE,
                                newEndTime,
                                request.getStartTime());

        if (conflict) {
            throw new ShowtimeConflictException("Update failed! This showtime clashes with another movie's schedule.");
        }

        // Update full
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(newEndTime);
        showtime.setBasePrice(request.getPrice());
        showtime.setFormat(request.getFormat());
        showtime.setStatus(request.getStatus());

        Showtime updated = showtimeRepository.save(showtime);

        return responseDTO(updated, movie, room);
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
                showtimeRepository.findByStartTimeBetween(startOfDay, endOfDay);

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

    @Override
    public List<ShowtimeResponse> getAvailableShowtime(Long movieId, LocalDate date) {

        // 1. Validate movie
        movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        // 2. Convert date → time range
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // 3. Query only ACTIVE
        List<Showtime> showtimes =
                showtimeRepository.findByMovieMovieIdAndStartTimeBetweenAndStatus(
                        movieId,
                        startOfDay,
                        endOfDay,
                        ShowtimeStatus.ACTIVE
                );

        // 4. Map DTO
        return showtimes.stream()
                .map(s -> responseDTO(s, s.getMovie(), s.getTheaterRoom()))
                .toList();
    }

    @Override
    public List<ShowtimeResponse> searchShowtime(
            Long movieId,
            Long roomId,
            LocalDate date,
            ShowtimeStatus status,
            String keyword
    ) {

        List<Showtime> showtimes = showtimeRepository.findAll();

        if (movieId != null) {
            showtimes = showtimes.stream()
                    .filter(s -> s.getMovie().getMovieId().equals(movieId))
                    .toList();
        }

        if (roomId != null) {
            showtimes = showtimes.stream()
                    .filter(s -> s.getTheaterRoom().getRoomId().equals(roomId))
                    .toList();
        }

        if (date != null) {
            showtimes = showtimes.stream()
                    .filter(s -> s.getStartTime().toLocalDate().equals(date))
                    .toList();
        }

        if (status != null) {
            showtimes = showtimes.stream()
                    .filter(s -> s.getStatus() == status)
                    .toList();
        }

        if (keyword != null && !keyword.isEmpty()) {
            showtimes = showtimes.stream()
                    .filter(s -> s.getMovie().getTitle().toLowerCase()
                            .contains(keyword.toLowerCase()))
                    .toList();
        }

        return showtimes.stream()
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
        response.setMovieId(movie.getMovieId());
        response.setMovieTitle(movie.getTitle());
        response.setTheaterRoomId(room.getRoomId());
        response.setTheaterRoomName(room.getRoomName());
        response.setStartTime(savedShowtime.getStartTime());
        response.setEndTime(savedShowtime.getEndTime());
        response.setFormat(savedShowtime.getFormat());
        response.setPrice(savedShowtime.getBasePrice());
        response.setStatus(savedShowtime.getStatus());

        return response;
    }
}