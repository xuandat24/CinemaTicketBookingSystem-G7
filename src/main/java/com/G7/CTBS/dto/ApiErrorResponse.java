package com.G7.CTBS.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Chỉ gửi các trường có dữ liệu
public class ApiErrorResponse {
    private int code;           // Mã lỗi (ví dụ: 400, 404, 500)
    private String message;     // Thông báo lỗi tổng quát
    private List<FieldError> errors; // Danh sách chi tiết các trường bị lỗi (dành cho Validation)

    @Data
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String defaultMessage;
    }
}