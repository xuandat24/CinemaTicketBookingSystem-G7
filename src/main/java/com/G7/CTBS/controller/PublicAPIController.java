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
    
    // 2. Lấy danh sách phim
    @GetMapping("/movies")
    public ResponseEntity<List<MovieDTO>> getAllMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "newest") String sortBy) {
        
        // 1. Chuyển đổi từ khóa sortBy của Frontend sang chuẩn của Backend
        String backendSortBy = "id"; // Mặc định
        if ("newest".equalsIgnoreCase(sortBy)) {
            backendSortBy = "releaseDateDesc"; // Mới nhất lên đầu
        } else if ("oldest".equalsIgnoreCase(sortBy)) {
            backendSortBy = "releaseDateAsc";  // Cũ nhất lên đầu
        }
        
        // 2. Gọi hàm search
        List<MovieDTO> movies = movieService.searchAndFilterMovies(title, categoryId, null, null, backendSortBy);
        
        // 3. Loại bỏ các phim đã bị xóa mềm (Disabled) trước khi gửi cho người dùng
        List<MovieDTO> activeMovies = movies.stream()
                .filter(m -> !"Disabled".equalsIgnoreCase(m.getStatus()))
                .toList();
        
        return ResponseEntity.ok(activeMovies);
    }
    
    // 3. Lấy chi tiết 1 bộ phim theo ID
    @GetMapping("/movies/{id}")
    public ResponseEntity<MovieDTO> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.findById(id));
    }
}