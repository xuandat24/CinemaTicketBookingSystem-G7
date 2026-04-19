package com.G7.CTBS.repository;

import com.G7.CTBS.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    @Query(value = """
            SELECT bs.seat_id
            FROM booking_seats bs
            INNER JOIN bookings b ON b.booking_id = bs.booking_id
            WHERE bs.showtime_id = :showtimeId
              AND UPPER(ISNULL(b.status, '')) IN ('CONFIRMED', 'SUCCESS')
            """, nativeQuery = true)
    List<Long> findBookedSeatIdsByShowtimeId(@Param("showtimeId") Long showtimeId);
}