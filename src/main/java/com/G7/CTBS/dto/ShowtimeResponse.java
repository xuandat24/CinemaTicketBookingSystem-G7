package com.G7.CTBS.dto;

import com.G7.CTBS.enums.ShowtimeFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShowtimeResponse {
    private Long showtimeId;
    private Long movieId;
    private String movieTitle;
    private Long theaterRoomId;
    private String theaterRoomName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ShowtimeFormat format;
    private Double price;
}