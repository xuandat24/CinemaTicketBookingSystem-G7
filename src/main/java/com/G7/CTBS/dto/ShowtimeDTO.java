package com.G7.CTBS.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeDTO {
    private Long showtimeId;
    private Long movieId; // ID phim
    private Long roomId;  // ID phòng chiếu
    private LocalDateTime startTime;
    private Double basePrice;
}
