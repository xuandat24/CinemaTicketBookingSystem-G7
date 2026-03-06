package com.G7.CTBS.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatDTO {
    private Long seatId;
    private Long roomId; // ID của phòng chiếu
    private String seatCode;
    private String seatType;
    private Double priceFactor;
}
