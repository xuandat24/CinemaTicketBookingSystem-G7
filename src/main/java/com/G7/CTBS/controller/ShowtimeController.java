package com.G7.CTBS.controller;

import com.G7.CTBS.dto.ShowtimeSeatResponseDTO;
import com.G7.CTBS.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {
    private final SeatService seatService;

    public ShowtimeController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping("/{showtimeId}/seats")
    public ResponseEntity<ShowtimeSeatResponseDTO> getSeats(@PathVariable Long showtimeId) {
        return ResponseEntity.ok(seatService.getSeatsForShowtime(showtimeId));
    }
}
