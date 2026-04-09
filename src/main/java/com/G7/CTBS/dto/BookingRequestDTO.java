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

    // Nên có thêm giá gốc để đối chiếu
    private Double originalPrice;

    // Giá cuối cùng sau khi đã trừ coupon (Cái này Huy đã có)
    private Double finalPrice;

    // Số tiền mà mã giảm giá đã trừ (Optional nhưng nên có)
    private Double discountAmount;

    private List<ComboSelection> combos;

    // Mã coupon khách hàng nhập vào (Đã thêm)
    private String couponCode;

    @Data
    public static class ComboSelection {
        private Long comboId;
        private Integer quantity;
    }
}