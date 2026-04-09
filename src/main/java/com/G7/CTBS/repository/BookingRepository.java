package com.G7.CTBS.repository;

import com.G7.CTBS.dto.MovieRevenueDTO;
import com.G7.CTBS.entity.Booking;
import com.G7.CTBS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByShowtimeShowtimeId(Long showtimeId);

    List<Booking> findByUserOrderByCreateTimeDesc(User user);

    List<Booking> findByStatusInAndCreateTimeBetween(Collection<String> statuses, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(b.finalPrice) FROM Booking b WHERE b.status IN ('SUCCESS', 'CONFIRMED') AND b.createTime BETWEEN :start AND :end")
    Double getTotalRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(bs) FROM Booking b JOIN b.bookingSeats bs WHERE b.status IN ('SUCCESS', 'CONFIRMED') AND b.createTime BETWEEN :start AND :end")
    Long getTotalTickets(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.G7.CTBS.dto.MovieRevenueDTO(m.title, SUM(b.finalPrice)) " +
            "FROM Booking b JOIN b.showtime s JOIN s.movie m " +
            "WHERE b.status IN ('SUCCESS', 'CONFIRMED') AND b.createTime BETWEEN :start AND :end " +
            "GROUP BY m.movieId, m.title " +
            "ORDER BY SUM(b.finalPrice) DESC")
    List<MovieRevenueDTO> getTopMoviesByRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(bs) FROM Booking b JOIN b.bookingSeats bs " +
            "WHERE b.showtime.showtimeId = :showtimeId AND b.status IN ('SUCCESS', 'CONFIRMED')")
    Long countBookedSeatsByShowtime(@Param("showtimeId") Long showtimeId);
}
