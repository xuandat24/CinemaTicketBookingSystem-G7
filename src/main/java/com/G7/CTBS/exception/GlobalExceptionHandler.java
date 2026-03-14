package com.G7.CTBS.exception;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 1. Xử lý lỗi Validation (Lỗi khi các annotation như @NotBlank, @Size trong DTO bị vi phạm)
     * Trả về mã 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        
        // Lấy câu message lỗi đầu tiên được định nghĩa trong DTO
        String errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        
        errorResponse.put("message", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 2. Xử lý lỗi không tìm thấy dữ liệu (Ví dụ: ID phim hoặc ID thể loại không tồn tại)
     * Trả về mã 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFoundException(EntityNotFoundException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        
        errorResponse.put("message", ex.getMessage()); // Lấy message từ lúc throw ở Service
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * 3. Xử lý lỗi vi phạm ràng buộc khóa ngoại (Foreign Key Constraint)
     * Cực kỳ quan trọng khi bạn XÓA một Category nhưng đang có Movie tham chiếu tới nó.
     * Trả về mã 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        
        // Ghi đè câu thông báo lỗi khô khan của Database bằng câu thân thiện với người dùng
        errorResponse.put("message", "Cannot delete this record because it is currently in use by other data.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * 4. Xử lý các lỗi RuntimeException tự định nghĩa trong Service
     * Trả về mã 400 Bad Request
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 5. Fallback: Bắt tất cả các lỗi hệ thống không lường trước được (NullPointerException, rớt mạng DB...)
     * Trả về mã 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex) {
        Map<String, String> errorResponse = new HashMap<>();
        
        // Không nên trả nguyên cái Exception ra Frontend vì lý do bảo mật, chỉ hiện câu báo chung chung
        errorResponse.put("message", "An unexpected server error occurred. Please try again later.");
        
        // (Tùy chọn) In lỗi thực sự ra console của Backend để dev dễ debug
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * 6. Xử lý lỗi trùng lặp dữ liệu (Ví dụ: Thêm mới một Thể loại đã tồn tại)
     * Trả về mã 409 Conflict
     */
    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<Map<String, String>> handleEntityExistsException(EntityExistsException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        
        // Lấy thông báo lỗi được ném ra từ tầng Service
        errorResponse.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * 7. Xử lý lỗi upload file quá dung lượng (Ví dụ: Trailer video quá nặng)
     * Trả về mã 413 Payload Too Large
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException() {
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "File upload error: Max file upload size exceeded (>500MB).");
        return ResponseEntity.status(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }
}