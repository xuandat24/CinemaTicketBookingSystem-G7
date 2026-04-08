package com.G7.CTBS.dto;

import com.G7.CTBS.enums.ShowtimeFormat;
import com.G7.CTBS.enums.ShowtimeStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "Price is required")
    @Min(value = 10000, message = "Price must be greater than 10,000")
    private Double price;

    private ShowtimeFormat format;
    private ShowtimeStatus status;
}