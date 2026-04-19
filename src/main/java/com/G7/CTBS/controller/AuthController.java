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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ==========================================
    // 1. API ĐĂNG KÝ
    // ==========================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid UserCreateRequest request) {
        try {
            userService.create(request);
            return ResponseEntity.ok(Map.of("message", "Registration successful!"));
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            String fieldName = errorMsg.toLowerCase().contains("email") ? "email" : "userName";

            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "errors", List.of(Map.of(
                            "field", fieldName,
                            "defaultMessage", errorMsg
                    ))
            ));
        }
    }

    // ==========================================
    // 2. API ĐĂNG NHẬP
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> processLogin(@RequestBody AuthenticationRequest request,
                                          HttpServletResponse response) {
        try {
            AutheticationResponse authResponse = authenticationService.authenticated(request);
            String token = authResponse.getToken();

            // TẠO COOKIE BẢO MẬT: Chìa khóa dự phòng cho trình duyệt tải trang tĩnh
            Cookie jwtCookie = new Cookie("jwtToken", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(0);
            response.addCookie(jwtCookie);

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            System.err.println(">> Login error: " + e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect username or password!"));
        }
    }

    // ==========================================
    // 3. API XÁC THỰC OTP (CHO ĐĂNG KÝ GOOGLE)
    // ==========================================
    @PostMapping("/verify-google-otp")
    public ResponseEntity<?> verify(@RequestParam("otp") String userOtp, HttpSession session) {
        String serverOtp = (String) session.getAttribute("OTP_CODE");
        Long createTime = (Long) session.getAttribute("OTP_TIME");

        if (serverOtp == null || createTime == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Verification code does not exist!"));
        }

        if (System.currentTimeMillis() - createTime > 300000) {
            session.removeAttribute("OTP_CODE");
            session.removeAttribute("PENDING_USER_DATA");
            return ResponseEntity.badRequest().body(Map.of("message", "OTP code has expired!"));
        }

        if (serverOtp.equals(userOtp)) {
            UserCreateRequest userData = (UserCreateRequest) session.getAttribute("PENDING_USER_DATA");

            if (userData != null) {
                User authenticatedUser = userRepository.findByEmail(userData.getEmail()).orElse(null);

                if (authenticatedUser == null) {
                    try {
                        authenticatedUser = userService.create(userData);
                    } catch (RuntimeException ex) {
                        authenticatedUser = userRepository.findByEmail(userData.getEmail()).orElse(null);
                        if (authenticatedUser == null) {
                            return ResponseEntity.badRequest().body(Map.of(
                                    "message", ex.getMessage() != null ? ex.getMessage() : "Unable to create account!"
                            ));
                        }
                    }
                }

                String rawName = authenticatedUser.getUserName();
                if (rawName == null || rawName.isBlank()) {
                    rawName = authenticatedUser.getEmail();
                }
                String displayName = rawName.contains("@") ? rawName.substring(0, rawName.indexOf('@')) : rawName;

                String token = authenticationService.tokenGeneration(rawName);

                session.removeAttribute("PENDING_USER_DATA");
                session.removeAttribute("OTP_CODE");
                session.removeAttribute("OTP_TIME");

                return ResponseEntity.ok(Map.of(
                        "message", "Verification and account creation successful!",
                        "token", token,
                        "username", displayName
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Registration data not found!"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Incorrect OTP code!"));
        }
    }

    // ==========================================
    // 4. API QUÊN MẬT KHẨU (GỬI OTP)
    // ==========================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "This email is not registered in the system!"));
        }

        User user = userOpt.get();
        if ("GOOGLE".equals(user.getProvider())) {
            return ResponseEntity.status(409).body(Map.of(
                    "type", "GOOGLE_ACCOUNT",
                    "message", "Your account is linked with Google. Please use Google's password recovery feature."
            ));
        }

        String otp = String.valueOf(new java.util.Random().nextInt(899999) + 100000);
        session.setAttribute("RESET_OTP_" + email, otp);
        session.setAttribute("RESET_OTP_TIME_" + email, System.currentTimeMillis());

        try {
            emailService.sendOtpEmail(email, otp);
            return ResponseEntity.ok(Map.of(
                    "type", "LOCAL_ACCOUNT",
                    "message", "An OTP code has been sent to your email."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error sending email, please try again later."));
        }
    }

    // ==========================================
    // 5. API ĐẶT LẠI MẬT KHẨU
    // ==========================================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        String sessionOtp = (String) session.getAttribute("RESET_OTP_" + email);
        if (sessionOtp == null || !sessionOtp.equals(otp)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Incorrect OTP code!"));
        }

        Long otpTime = (Long) session.getAttribute("RESET_OTP_TIME_" + email);
        if (otpTime == null || System.currentTimeMillis() - otpTime > 300000) {
            session.removeAttribute("RESET_OTP_" + email);
            return ResponseEntity.badRequest().body(Map.of("message", "OTP code has expired! Please request a new one."));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            userService.updatePassword(userOpt.get(), newPassword);

            session.removeAttribute("RESET_OTP_" + email);
            session.removeAttribute("RESET_OTP_TIME_" + email);

            return ResponseEntity.ok(Map.of("message", "Password reset successful! You can now log in."));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "An error occurred, please try again!"));
    }

    // ==========================================
    // 6. API ĐĂNG XUẤT (LOGOUT)
    // ==========================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. Hủy Session Server
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // 2. Xóa danh tính Security
        SecurityContextHolder.clearContext();

        // 3. VÁ LỖI BẢO MẬT: Xóa triệt để Cookie JSESSIONID và Cookie jwtToken
        Cookie sessionCookie = new Cookie("JSESSIONID", null);
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setMaxAge(0);
        response.addCookie(sessionCookie);

        Cookie jwtCookie = new Cookie("jwtToken", null);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        return ResponseEntity.ok(Map.of("message", "Logout successful and Session/Cookie cleared"));
    }
}
