package com.G7.CTBS.repository;

import com.G7.CTBS.entity.TheaterRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TheaterRoomRepository extends JpaRepository<TheaterRoom, Long> {
}
