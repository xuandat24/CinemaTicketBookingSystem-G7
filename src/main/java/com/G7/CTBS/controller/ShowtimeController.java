package com.G7.CTBS.controller;

import com.G7.CTBS.dto.*;
import com.G7.CTBS.enums.ShowtimeStatus;
import com.G7.CTBS.service.ShowtimeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {
    private final ShowtimeService showtimeService;

    public ShowtimeController(
            ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    // Create
    @PostMapping()
    public ResponseEntity<ShowtimeResponse> createShowtime(
            @Valid @RequestBody CreateShowtimeRequest request) {

        ShowtimeResponse response = showtimeService.createShowtime(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> updateShowtime(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShowtimeRequest request){

        ShowtimeResponse response = showtimeService.updateShowtime(id, request);

        return ResponseEntity.ok(response);
    }

    // Get by id
    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> getShowtimeById(
            @PathVariable Long id){

        return ResponseEntity.ok(showtimeService.getShowtimeById(id));
    }

    // Get by movie
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowtimeResponse>> getByMovie(
            @PathVariable Long movieId){

        return ResponseEntity.ok(
                showtimeService.getShowtimeByMovie(movieId));
    }

    // Get by room
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ShowtimeResponse>> getByRoom(
            @PathVariable Long roomId){

        return ResponseEntity.ok(
                showtimeService.getShowtimeByRoom(roomId));
    }

    // Get by date
    @GetMapping("/date")
    public ResponseEntity<List<ShowtimeResponse>> getByDate(
            @RequestParam LocalDate date){

        return ResponseEntity.ok(showtimeService.getShowtimeByDate(date));
    }

    // Get only active showtime
    @GetMapping("/available")
    public List<ShowtimeResponse> getAvailableShowtime(
            @RequestParam Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return showtimeService.getAvailableShowtime(movieId, date);
    }

    // Get all
    @GetMapping
    public ResponseEntity<List<ShowtimeResponse>> getAll(){

        return ResponseEntity.ok(showtimeService.getAllShowtime());
    }

    // Search and filter
    @GetMapping("/search")
    public ResponseEntity<List<ShowtimeResponse>> searchShowtime(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ShowtimeStatus status
    ) {
        return ResponseEntity.ok(
                showtimeService.searchShowtime(movieId, roomId, date, status)
        );
    }

}