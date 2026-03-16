package com.G7.CTBS.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheaterRoomDTO {
    private Long roomId;
    private String roomName;
    private Integer totalSeats;
}
