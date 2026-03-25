package com.G7.CTBS.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequestDTO {
    private Long showtimeId;
    private List<Long> seatIds;
    private Long userId;
    private Double finalPrice; // Đã đổi theo Entity
    private List<ComboSelection> combos;

    @Data
    public static class ComboSelection {
        private Long comboId;
        private Integer quantity;
    }
}