package com.G7.CTBS.exception;

import com.G7.CTBS.dto.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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

    /**
     * 8. InvalidShowtimeException
     * Lỗi dữ liệu showtime không hợp lệ (ví dụ: startTime trong quá khứ)
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(InvalidShowtimeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidShowtimeException(InvalidShowtimeException ex) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("error", "Invalid Showtime");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 9. ShowtimeConflictException
     * Lỗi trùng lịch chiếu trong cùng phòng
     * HTTP 409 - Conflict
     */
    @ExceptionHandler(ShowtimeConflictException.class)
    public ResponseEntity<Map<String, Object>> handleShowtimeConflictException(ShowtimeConflictException ex) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", 409);
        response.put("error", "Showtime Conflict");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 10. ShowtimeHasBookingException
     * Không thể sửa/xóa showtime vì đã có booking
     * HTTP 409 - Conflict
     */
    @ExceptionHandler(ShowtimeHasBookingException.class)
    public ResponseEntity<Map<String, Object>> handleShowtimeHasBookingException(ShowtimeHasBookingException ex) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", 409);
        response.put("error", "Showtime Has Booking");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 11. ResourceNotFoundException
     * Không thể tim thay resource
     * HTTP 409 - Conflict
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", 404);
        response.put("error", "Resource Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}