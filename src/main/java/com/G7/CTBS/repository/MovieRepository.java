package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByStatus(String status);

    @Query("SELECT DISTINCT m FROM Movie m JOIN m.showtimes s WHERE m.status = 'Pending' AND s.startTime <= :now")
    List<Movie> findMoviesToUpdateStatus(@Param("now") LocalDateTime now);
}
