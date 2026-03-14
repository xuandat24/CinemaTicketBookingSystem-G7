package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Movie;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByStatus(String status);
    
    List<Movie> findByTitleContainingIgnoreCase(String title);
    
    List<Movie> findByCategories_NameContainingIgnoreCase(String categoryName);
    
    boolean existsByTitleIgnoreCase(String title);
    
    boolean existsByTitleIgnoreCaseAndMovieIdNot(String title, Long movieId);
    
    @Query("SELECT DISTINCT m FROM Movie m " +
            "JOIN m.showtimes s " +
            "WHERE m.status = 'Coming Soon' AND s.startTime <= :now")
    List<Movie> findMoviesToUpdateStatus(LocalDateTime now);
    
    @Query("SELECT m FROM Movie m " +
            "WHERE m.status = 'Now Playing' AND NOT EXISTS (" +
            "SELECT s FROM Showtime s WHERE s.movie = m AND s.startTime >= :now)")
    List<Movie> findMoviesToSuspend(LocalDateTime now);
    
    @Query("SELECT m FROM Movie m " +
            "WHERE (:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:categoryId IS NULL OR EXISTS (SELECT 1 FROM m.categories c WHERE c.categoryId = :categoryId)) " +
            "AND (CAST(:fromDate AS date) IS NULL OR m.releaseDate >= :fromDate) " +
            "AND (CAST(:toDate AS date) IS NULL OR m.releaseDate <= :toDate)")
    List<Movie> searchMovies(@Param("title") String title,
                             @Param("categoryId") Long categoryId,
                             @Param("fromDate") java.time.LocalDate fromDate,
                             @Param("toDate") java.time.LocalDate toDate,
                             org.springframework.data.domain.Sort sort);
}
