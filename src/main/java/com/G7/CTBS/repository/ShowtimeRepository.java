package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Showtime;
import com.G7.CTBS.enums.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieMovieId(Long movieId);

    List<Showtime> findByTheaterRoomRoomId(Long roomId);

    List<Showtime> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Showtime> findByMovieMovieIdAndStartTimeBetweenAndStatus(
            Long movieId,
            LocalDateTime start,
            LocalDateTime end,
            ShowtimeStatus status
    );

    List<Showtime> findByTheaterRoomRoomIdAndStatus(
            Long roomId,
            ShowtimeStatus status
    );

    List<Showtime> findByStatus(ShowtimeStatus status);

    boolean existsByTheaterRoomRoomIdAndStatusAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            Long roomId,
            ShowtimeStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    boolean existsByTheaterRoomRoomIdAndShowtimeIdNotAndStatusAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            Long roomId,
            Long showtimeId,
            ShowtimeStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
    boolean existsByTheaterRoom_RoomIdAndStartTimeAfter(Long roomId, LocalDateTime startTime);

    @Query("SELECT s FROM Showtime s WHERE s.startTime BETWEEN :start AND :end " +
            "AND (:keyword IS NULL OR LOWER(s.movie.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Showtime> findShowtimesForOccupancy(@Param("start") java.time.LocalDateTime start,
                                             @Param("end") java.time.LocalDateTime end,
                                             @Param("keyword") String keyword);
}
