package com.G7.CTBS.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeatDTO {
    private Long bookingSeatId;
    private Long bookingId;
    private Long showtimeId;
    private Long seatId;
}
