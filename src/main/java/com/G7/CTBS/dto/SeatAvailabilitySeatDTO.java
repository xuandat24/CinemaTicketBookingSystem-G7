package com.G7.CTBS.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatAvailabilitySeatDTO {
    private Long seatId;
    private String seatCode;
    private String seatType;
    private Double priceFactor;
    private boolean booked;
}