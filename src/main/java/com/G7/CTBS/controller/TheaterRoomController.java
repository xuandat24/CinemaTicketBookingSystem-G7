package com.G7.CTBS.controller;

import com.G7.CTBS.dto.TheaterRoomDTO;
import com.G7.CTBS.repository.TheaterRoomRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/theater-rooms", "/api/rooms"})
public class TheaterRoomController {
    private final TheaterRoomRepository theaterRoomRepository;
    
    public TheaterRoomController(TheaterRoomRepository theaterRoomRepository) {
        this.theaterRoomRepository = theaterRoomRepository;
    }
    
    @GetMapping
    public List<TheaterRoomDTO> getAllTheaterRooms() {
        return theaterRoomRepository.findAll(Sort.by(Sort.Direction.ASC, "roomId"))
                .stream()
                .map(room -> TheaterRoomDTO.builder()
                        .roomId(room.getRoomId())
                        .roomName(room.getRoomName())
                        .totalSeats(room.getTotalSeats())
                        .build())
                .toList();
    }
}