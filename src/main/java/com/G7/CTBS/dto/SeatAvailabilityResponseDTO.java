package com.G7.CTBS.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatAvailabilityResponseDTO {
    private Long showtimeId;
    private Long roomId;
    private String roomName;
    private Long movieId;
    private String movieTitle;
    private String bannerPath;
    private Integer duration;
    private String genre;
    private LocalDateTime startTime;
    private Double basePrice;
    private List<SeatAvailabilitySeatDTO> seats;
}
