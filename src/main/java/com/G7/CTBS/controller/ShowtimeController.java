package com.G7.CTBS.controller;

import com.G7.CTBS.dto.*;
import com.G7.CTBS.service.ShowtimeService;
import jakarta.validation.Valid;
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

    // create
    @PostMapping()
    public ResponseEntity<ShowtimeResponse> createShowtime(
            @Valid @RequestBody CreateShowtimeRequest request) {

        ShowtimeResponse response = showtimeService.createShowtime(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // update
    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> updateShowtime(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShowtimeRequest request){

        ShowtimeResponse response = showtimeService.updateShowtime(id, request);

        return ResponseEntity.ok(response);
    }

    // delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable Long id){

        showtimeService.deleteShowtime(id);

        return ResponseEntity.noContent().build();
    }

    // get by id
    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> getShowtimeById(
            @PathVariable Long id){

        return ResponseEntity.ok(showtimeService.getShowtimeById(id));
    }

    // get by movie
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowtimeResponse>> getByMovie(
            @PathVariable Long movieId){

        return ResponseEntity.ok(
                showtimeService.getShowtimeByMovie(movieId));
    }

    // get by room
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ShowtimeResponse>> getByRoom(
            @PathVariable Long roomId){

        return ResponseEntity.ok(
                showtimeService.getShowtimeByRoom(roomId));
    }

    // get by date
    @GetMapping("/date")
    public ResponseEntity<List<ShowtimeResponse>> getByDate(
            @RequestParam LocalDate date){

        return ResponseEntity.ok(showtimeService.getShowtimeByDate(date));
    }

    // get all
    @GetMapping
    public ResponseEntity<List<ShowtimeResponse>> getAll(){

        return ResponseEntity.ok(showtimeService.getAllShowtime());
    }
}