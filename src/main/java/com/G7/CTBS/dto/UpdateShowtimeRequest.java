package com.G7.CTBS.dto;

import com.G7.CTBS.enums.ShowtimeFormat;
import com.G7.CTBS.enums.ShowtimeStatus;
import jakarta.validation.constraints.Future;
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
public class UpdateShowtimeRequest {
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @Positive(message = "Price must be greater than 0")
    private Double price;

    private ShowtimeFormat format;
    private ShowtimeStatus status;
}