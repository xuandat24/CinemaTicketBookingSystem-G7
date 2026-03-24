package com.G7.CTBS.exception;

import com.G7.CTBS.dto.ApiErrorResponse;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException; // THÊM IMPORT NÀY CỦA ĐỒNG ĐỘI
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // =================================================================================
    // 1. LỖI VALIDATION (Giữ nguyên của bạn để Frontend hiện lỗi đỏ từng ô Input)
    // =================================================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ApiErrorResponse.FieldError> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Dữ liệu đầu vào không hợp lệ")
                .errors(details)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    // =================================================================================
    // 2. LỖI RUNTIME CHUNG (Trùng tên người dùng, sđt... - Dùng chung cho cả 2)
    // =================================================================================
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex) {
        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    // =================================================================================
    // 3. LỖI KHÔNG TÌM THẤY DỮ LIỆU (Của đồng đội)
    // =================================================================================
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // =================================================================================
    // 4. LỖI VI PHẠM KHÓA NGOẠI (Của đồng đội - VD: Xóa Category đang có Phim)
    // =================================================================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(HttpStatus.CONFLICT.value())
                .message("Cannot delete this record because it is currently in use by other data.")
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // =================================================================================
    // 5. LỖI TRÙNG LẶP DỮ LIỆU BẬC DATABASE (Của đồng đội)
    // =================================================================================
    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityExistsException(EntityExistsException ex) {
        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(HttpStatus.CONFLICT.value())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // =================================================================================
    // 6. LỖI FILE UPLOAD QUÁ LỚN (Của đồng đội)
    // =================================================================================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .message("File upload error: Max file upload size exceeded (>500MB).")
                .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    // =================================================================================
    // 7. LỖI NHẬP SAI ĐỊNH DẠNG NGÀY THÁNG/JSON (Của bạn)
    // =================================================================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Định dạng dữ liệu không hợp lệ. Vui lòng không nhập năm quá 4 chữ số.")
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    // =================================================================================
    // 8. LỖI 404 RESOURCE (Của bạn - Trả về rỗng)
    // =================================================================================
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    // =================================================================================
    // 9. LỖI SERVER CHUNG (Gộp lại: Báo lỗi an toàn ra Frontend + Log lỗi ra Console)
    // =================================================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex) {
        // In lỗi ra console để Developer dễ Debug (Theo ý đồng đội)
        ex.printStackTrace();

        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                // Che giấu lỗi thật, chỉ báo lỗi chung chung ra Frontend để bảo mật (Theo ý đồng đội)
                .message("An unexpected server error occurred. Please try again later.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}