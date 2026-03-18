package com.G7.CTBS.exception;

import com.G7.CTBS.dto.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Xử lý lỗi Validation (@Valid - sai định dạng, bỏ trống...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ApiErrorResponse.FieldError> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(400)
                .message("Dữ liệu đầu vào không hợp lệ")
                .errors(details)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    // 2. Xử lý các lỗi logic nghiệp vụ tự ném ra (RuntimeException - ví dụ: Trùng tên người dùng)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex) {
        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(400)
                .message(ex.getMessage()) // "Tên người dùng đã được sử dụng"
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    // 3. Xử lý tất cả các lỗi hệ thống còn lại
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex) {
        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(500)
                .message("Có lỗi hệ thống xảy ra: " + ex.getMessage())
                .build();

        return ResponseEntity.internalServerError().body(error);
    }
}