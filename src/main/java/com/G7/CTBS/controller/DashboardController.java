package com.G7.CTBS.controller;

import com.G7.CTBS.dto.DashboardResponseDTO;
import com.G7.CTBS.dto.ShowtimeOccupancyDTO;
import com.G7.CTBS.repository.MovieRepository;
import com.G7.CTBS.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private MovieRepository movieRepository;
    private DashboardService dashboardService;

    @Autowired
    public DashboardController(MovieRepository movieRepository, DashboardService dashboardService) {
        this.movieRepository = movieRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponseDTO> getDashboardData(@RequestParam(defaultValue = "day") String filter) {
        return ResponseEntity.ok(dashboardService.getDashboardData(filter));
    }

    @GetMapping("/occupancy")
    public ResponseEntity<List<ShowtimeOccupancyDTO>> getOccupancy(
            @RequestParam(defaultValue = "day") String filter,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(dashboardService.getOccupancyData(filter, keyword));
    }

    @GetMapping("/movies-list")
    public ResponseEntity<List<java.util.Map<String, Object>>> getMoviesForFilter() {
        List<java.util.Map<String, Object>> list = movieRepository.findAll().stream().map(m -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", m.getMovieId());
            map.put("title", m.getTitle());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(list);
    }
}