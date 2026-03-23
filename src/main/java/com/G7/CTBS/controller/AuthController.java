package com.G7.CTBS.controller;

import com.G7.CTBS.dto.AuthenticationRequest;
import com.G7.CTBS.dto.AutheticationResponse;
import com.G7.CTBS.dto.UserCreateRequest;
import com.G7.CTBS.entity.User;
import com.G7.CTBS.service.AuthenticationService;
import com.G7.CTBS.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController // BẮT BUỘC dùng RestController khi làm việc với API/JSON
@RequestMapping("/api/auth")
@RequiredArgsConstructor // Tự động tạo Constructor cho UserService và AuthenticationService
public class AuthController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    // 1. API Đăng ký (Đã chạy tốt)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid UserCreateRequest request) {
        try {
            userService.create(request);
            return ResponseEntity.ok("Register successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. API ĐĂNG NHẬP (BỔ SUNG ĐOẠN NÀY)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request) {
        try {
            // Gọi service để kiểm tra tài khoản/mật khẩu và tạo JWT Token
            AutheticationResponse response = authenticationService.authenticated(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Nếu sai mật khẩu hoặc tài khoản không tồn tại, trả về lỗi 400
            return ResponseEntity.badRequest().body(Map.of("message", "Incorrect password!"));
        }
    }

    // 2. API Đăng nhập (Login)
    @PostMapping("/verify-google-otp")
    public ResponseEntity<?> verify(@RequestParam("otp") String userOtp, HttpSession session) {
        // 1. Lấy dữ liệu từ Session
        System.out.println("Verify - Session ID: " + session.getId());
        String serverOtp = (String) session.getAttribute("OTP_CODE");
        Long createTime = (Long) session.getAttribute("OTP_TIME");

        // 2. Kiểm tra nếu Session mất hoặc hết hạn
        if (serverOtp == null || createTime == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Verify code invalid!"));
        }

        // 3. Kiểm tra hết hạn thực tế (5 phút)
        if (System.currentTimeMillis() - createTime > 300000) {
            session.removeAttribute("OTP_CODE");
            session.removeAttribute("PENDING_USER_DATA");
            return ResponseEntity.badRequest().body(Map.of("message", "Verify code expired!"));
        }

        // 4. So khớp OTP
        if (serverOtp.equals(userOtp)) {
            // Lấy thông tin user từ Session ra để lưu vào DB
            UserCreateRequest userData = (UserCreateRequest) session.getAttribute("PENDING_USER_DATA");

            if (userData != null) {
                // SỬA TẠI ĐÂY: Dùng biến instance 'userService' thay vì Class 'UserService'
                userService.create(userData);

                // Xóa dữ liệu tạm sau khi đăng ký thành công
                session.removeAttribute("PENDING_USER_DATA");
                session.removeAttribute("OTP_CODE");
                session.removeAttribute("OTP_TIME");

                return ResponseEntity.ok(Map.of("message", "Xác thực và tạo tài khoản thành công!"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy dữ liệu đăng ký!"));
        } else {
            // SỬA TẠI ĐÂY: Thêm return cho trường hợp OTP sai để hết lỗi "Missing return statement"
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không chính xác!"));
        }
    }
}