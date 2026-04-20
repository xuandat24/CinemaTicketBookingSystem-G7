package com.G7.CTBS.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeOccupancyDTO {
    private String movieName;
    private String roomName;
    private String time;
    private int totalSeats;
    private int bookedSeats;
    private double occupancyRate;
}