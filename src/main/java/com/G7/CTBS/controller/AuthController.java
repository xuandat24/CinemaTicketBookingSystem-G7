package com.G7.CTBS.controller;

import com.G7.CTBS.dto.AuthenticationRequest;
import com.G7.CTBS.dto.AutheticationResponse;
import com.G7.CTBS.dto.UserCreateRequest;
import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.UserRepository;
import com.G7.CTBS.service.AuthenticationService;
import com.G7.CTBS.service.EmailService;
import com.G7.CTBS.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController // BẮT BUỘC dùng RestController khi làm việc với API/JSON
@RequestMapping("/api/auth")
@RequiredArgsConstructor // Tự động tạo Constructor cho UserService và AuthenticationService
public class AuthController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    // 1. API Đăng ký (Đã chạy tốt)
    // 1. API Đăng ký
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid UserCreateRequest request) {
        try {
            userService.create(request);
            return ResponseEntity.ok(Map.of("message", "Đăng ký thành công!"));
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();

            // Tự động nhận diện lỗi thuộc về ô nhập nào dựa vào chữ trong câu lỗi
            String fieldName = errorMsg.toLowerCase().contains("email") ? "email" : "userName";

            // Trả về cấu trúc JSON chứa mảng errors để Frontend dễ map vào các ô input
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "errors", List.of(Map.of(
                            "field", fieldName,
                            "defaultMessage", errorMsg
                    ))
            ));
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
            return ResponseEntity.badRequest().body(Map.of("message", "Tên đăng nhập hoặc mật khẩu không chính xác!"));
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
            return ResponseEntity.badRequest().body(Map.of("message", "Mã xác thực không tồn tại!"));
        }

        // 3. Kiểm tra hết hạn thực tế (5 phút)
        if (System.currentTimeMillis() - createTime > 300000) {
            session.removeAttribute("OTP_CODE");
            session.removeAttribute("PENDING_USER_DATA");
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP đã hết hạn!"));
        }

        // 4. So khớp OTP
        if (serverOtp.equals(userOtp)) {
            // Lấy thông tin user từ Session ra để lưu vào DB
            UserCreateRequest userData = (UserCreateRequest) session.getAttribute("PENDING_USER_DATA");

            if (userData != null) {
                // SỬA TẠI ĐÂY: Dùng biến instance 'userService' thay vì Class 'UserService'
                userService.create(userData);

                // ==========================================
                // THÊM MỚI: TẠO TOKEN ĐỂ TỰ ĐỘNG LOGIN
                // ==========================================
                String rawName = userData.getUserName();
                if (rawName == null || rawName.isEmpty()) {
                    rawName = "Google User";
                }
                // Gọi service tạo token (giống hệt lúc đăng nhập)
                String token = authenticationService.tokenGeneration(rawName);

                // Xóa dữ liệu tạm sau khi đăng ký thành công
                session.removeAttribute("PENDING_USER_DATA");
                session.removeAttribute("OTP_CODE");
                session.removeAttribute("OTP_TIME");

                // SỬA TẠI ĐÂY: Trả về thêm 'token' và 'username' trong JSON
                return ResponseEntity.ok(Map.of(
                        "message", "Xác thực và tạo tài khoản thành công!",
                        "token", token,
                        "username", rawName
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy dữ liệu đăng ký!"));
        } else {
            // SỬA TẠI ĐÂY: Thêm return cho trường hợp OTP sai để hết lỗi "Missing return statement"
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không chính xác!"));
        }
    }
    // ==========================================
    // 3. API YÊU CẦU QUÊN MẬT KHẨU (GỬI OTP)
    // ==========================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");

        // Tìm user theo email
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email này chưa được đăng ký trong hệ thống!"));
        }

        User user = userOpt.get();

        // Kiểm tra xem có phải tài khoản Google không
        if ("GOOGLE".equals(user.getProvider())) {
            return ResponseEntity.status(409).body(Map.of(
                    "type", "GOOGLE_ACCOUNT",
                    "message", "Tài khoản của bạn được liên kết với Google. Vui lòng sử dụng tính năng khôi phục mật khẩu của Google."
            ));
        }

        // Nếu là tài khoản thường -> Tạo và gửi OTP
        String otp = String.valueOf(new java.util.Random().nextInt(899999) + 100000);

        // Lưu OTP vào session gắn với email để tránh nhầm lẫn
        session.setAttribute("RESET_OTP_" + email, otp);
        session.setAttribute("RESET_OTP_TIME_" + email, System.currentTimeMillis());

        try {
            emailService.sendOtpEmail(email, otp);
            return ResponseEntity.ok(Map.of(
                    "type", "LOCAL_ACCOUNT",
                    "message", "Mã OTP đã được gửi đến email của bạn."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi khi gửi email, vui lòng thử lại sau."));
        }
    }

    // ==========================================
    // 4. API ĐẶT LẠI MẬT KHẨU MỚI
    // ==========================================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        // 1. Lấy OTP từ Session ra để kiểm tra
        String sessionOtp = (String) session.getAttribute("RESET_OTP_" + email);
        if (sessionOtp == null || !sessionOtp.equals(otp)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không chính xác!"));
        }

        // 2. Kiểm tra OTP có hết hạn không (Giới hạn 5 phút = 300,000 ms)
        Long otpTime = (Long) session.getAttribute("RESET_OTP_TIME_" + email);
        if (otpTime == null || System.currentTimeMillis() - otpTime > 300000) {
            session.removeAttribute("RESET_OTP_" + email); // Xóa OTP cũ
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP đã hết hạn! Vui lòng gửi lại mã."));
        }

        // 3. Nếu mọi thứ đúng -> Tiến hành cập nhật mật khẩu
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            userService.updatePassword(userOpt.get(), newPassword);

            // Dọn dẹp Session cho sạch sẽ
            session.removeAttribute("RESET_OTP_" + email);
            session.removeAttribute("RESET_OTP_TIME_" + email);

            return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập ngay bây giờ."));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Có lỗi xảy ra, vui lòng thử lại!"));
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. Hủy Session hiện tại ở Server
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // 2. Xóa thông tin đăng nhập trong Security Context
        SecurityContextHolder.clearContext();

        // 3. Xóa triệt để Cookie JSESSIONID lưu trên trình duyệt
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // Set tuổi thọ về 0 để trình duyệt xóa ngay
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công và đã xóa Session"));
    }
}