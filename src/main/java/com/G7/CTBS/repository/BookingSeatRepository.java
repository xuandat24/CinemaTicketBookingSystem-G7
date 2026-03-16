package com.G7.CTBS.repository;

import com.G7.CTBS.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    @Query("SELECT bs.seat.seatId FROM BookingSeat bs WHERE bs.showtime.showtimeId = :showtimeId")
    List<Long> findBookedSeatIdsByShowtimeId(@Param("showtimeId") Long showtimeId);
}
