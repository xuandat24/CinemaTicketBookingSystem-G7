package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository  extends JpaRepository<Seat, Long> {
    long countByRoom_RoomId(Long roomId);
    boolean existsByRoom_RoomIdAndSeatCode(Long roomId, String seatCode);
    List<Seat> findByRoom_RoomId(Long roomId);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.room.roomId = :roomId")
    Long countSeatsByRoom(@Param("roomId") Long roomId);
}