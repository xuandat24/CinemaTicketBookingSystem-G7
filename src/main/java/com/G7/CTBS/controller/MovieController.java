package com.G7.CTBS.controller;

import com.G7.CTBS.dto.MovieDTO;
import com.G7.CTBS.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/movies")
public class MovieController {
    
    private final MovieService movieService;
    
    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }
    
    @GetMapping
    public ResponseEntity<List<MovieDTO>> getMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "id") String sortBy) {
        
        return ResponseEntity.ok(movieService.searchAndFilterMovies(title, categoryId, language, status, fromDate, toDate, sortBy));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MovieDTO> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.findById(id));
    }
    
    @PostMapping
    public ResponseEntity<Map<String, String>> createMovie(@Valid @ModelAttribute MovieDTO movieDTO) {
        movieService.createMovie(movieDTO);
        return ResponseEntity.ok(Map.of("message", "Movie added successfully!"));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateMovie(@PathVariable Long id, @Valid @ModelAttribute MovieDTO movieDTO) {
        movieDTO.setMovieId(id);
        movieService.updateMovie(movieDTO);
        return ResponseEntity.ok(Map.of("message", "Movie updated successfully!"));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.ok(Map.of("message", "Movie deleted successfully!"));
    }
}