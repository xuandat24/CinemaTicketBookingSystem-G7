package com.G7.CTBS.controller;

import com.G7.CTBS.dto.CategoryDTO;
import com.G7.CTBS.dto.MovieDTO;
import com.G7.CTBS.service.CategoryService;
import com.G7.CTBS.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicAPIController {
    private final MovieService movieService;
    private final CategoryService categoryService;
    
    @Autowired
    public PublicAPIController(MovieService movieService, CategoryService categoryService) {
        this.movieService = movieService;
        this.categoryService = categoryService;
    }
    
    // 1. Lấy danh sách tất cả danh mục phim
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }
    
    // 2. Lấy danh sách phim (ĐÃ TỐI ƯU HÓA)
    @GetMapping("/movies")
    public ResponseEntity<List<MovieDTO>> getAllMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "newest") String sortBy) {
        return ResponseEntity.ok(movieService.getPublicMovies(title, categoryId, language, status, sortBy));
    }
    
    @GetMapping("/movies/{id}")
    public ResponseEntity<MovieDTO> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.findById(id));
    }
}