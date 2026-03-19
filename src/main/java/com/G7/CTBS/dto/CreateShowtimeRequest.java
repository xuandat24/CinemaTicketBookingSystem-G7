package com.G7.CTBS.dto;

import com.G7.CTBS.enums.ShowtimeFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateShowtimeRequest {
    @NotNull(message = "Movie ID is required")
    private Long movieId;

    @NotNull(message = "Theater room ID is required")
    private Long theaterRoomId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    @NotNull(message = "Format is required")
    private ShowtimeFormat format;
}
