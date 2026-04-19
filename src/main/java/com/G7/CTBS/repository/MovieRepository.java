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

    boolean existsByTitleIgnoreCase(String title);

    @Query("SELECT m FROM Movie m " +
            "WHERE m.status = 'Now Playing' " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Showtime s WHERE s.movie = m " +
            "    AND s.status = 'ACTIVE' " +
            "    AND s.endTime >= :now" +
            ")")
    List<Movie> findMoviesToPending(@Param("now") LocalDateTime now);

    @Query("SELECT m FROM Movie m " +
            "WHERE m.status = 'Pending' " +
            "AND EXISTS (" +
            "    SELECT 1 FROM Showtime s WHERE s.movie = m " +
            "    AND s.status = 'ACTIVE' " +
            "    AND s.startTime <= :now AND s.endTime >= :now" +
            ")")
    List<Movie> findMoviesToPlay(@Param("now") LocalDateTime now);

    @Query("SELECT m FROM Movie m " +
            "WHERE (:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:categoryId IS NULL OR EXISTS (SELECT 1 FROM m.categories c WHERE c.categoryId = :categoryId)) " +
            "AND (:status IS NULL OR m.status = :status) " +
            "AND (CAST(:fromDate AS date) IS NULL OR m.releaseDate >= :fromDate) " +
            "AND (CAST(:toDate AS date) IS NULL OR m.releaseDate <= :toDate) " +
            "AND (:language IS NULL " +
            "     OR (:language = 'Other' AND m.language NOT IN ('Vietnamese', 'English', 'Korean', 'Japanese', 'Chinese', 'Thai')) " +
            "     OR (:language != 'Other' AND LOWER(m.language) LIKE LOWER(CONCAT('%', :language, '%'))))")
    List<Movie> searchMovies(@Param("title") String title,
                             @Param("categoryId") Long categoryId,
                             @Param("language") String language,
                             @Param("status") String status,
                             @Param("fromDate") java.time.LocalDate fromDate,
                             @Param("toDate") java.time.LocalDate toDate,
                             org.springframework.data.domain.Sort sort);
}
