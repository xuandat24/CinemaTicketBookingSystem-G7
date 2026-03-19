package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByShowtimeShowtimeId(Long showtimeId);
}
