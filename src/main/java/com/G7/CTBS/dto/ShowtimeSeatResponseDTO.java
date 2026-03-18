package com.G7.CTBS.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeSeatResponseDTO {
    private Long showtimeId;
    private Long roomId;
    private String roomName;
    private Double basePrice;
    private List<SeatAvailabilityDTO> seats;
}
