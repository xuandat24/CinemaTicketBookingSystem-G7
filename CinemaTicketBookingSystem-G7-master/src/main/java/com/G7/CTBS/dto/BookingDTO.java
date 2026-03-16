package com.G7.CTBS.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDTO {
    private Long bookingId;
    private Long userId;
    private Long showtimeId;
    private String bookingCode;
    private Double finalPrice;
    private String status;
    private LocalDateTime createTime;
    
    // Rất hữu ích khi API trả về chi tiết đơn đặt vé kèm các ghế đã đặt
    private List<Long> bookedSeatIds;
}
