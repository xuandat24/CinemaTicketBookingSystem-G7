package com.G7.CTBS.controller;

import com.G7.CTBS.dto.SeatAvailabilityResponseDTO;
import com.G7.CTBS.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{showtimeId}/seats")
    public ResponseEntity<SeatAvailabilityResponseDTO> getSeatsByShowtime(@PathVariable Long showtimeId) {
        return ResponseEntity.ok(seatService.getSeatAvailabilityByShowtimeId(showtimeId));
    }
}