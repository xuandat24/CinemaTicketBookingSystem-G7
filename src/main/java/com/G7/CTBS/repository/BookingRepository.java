package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Booking;
import com.G7.CTBS.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByShowtimeShowtimeId(Long showtimeId);
    List<Booking> findByUserOrderByCreateTimeDesc(User user);
}