package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Movie;
import com.G7.CTBS.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieMovieId(Long movieId);
    List<Showtime> findByTheaterRoomRoomId(Long roomId);
    List<Showtime> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByTheaterRoomRoomIdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            Long roomId,
            LocalDateTime start,
            LocalDateTime end);

    boolean existsByTheaterRoomRoomIdAndShowtimeIdNotAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            Long roomId,
            Long showtimeId,
            LocalDateTime end,
            LocalDateTime start);
}

