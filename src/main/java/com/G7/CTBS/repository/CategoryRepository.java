package com.G7.CTBS.repository;

import com.G7.CTBS.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByNameContainingIgnoreCase(String categoryName);
    Optional<Category> findByNameIgnoreCase(String categoryName);
    boolean existsByNameIgnoreCase(String categoryName);
    @Query("SELECT COUNT(m) FROM Movie m JOIN m.categories c WHERE c.categoryId = :categoryId AND m.status != 'Disabled'")
    Long countMoviesByCategoryId(@Param("categoryId") Long categoryId);
}
