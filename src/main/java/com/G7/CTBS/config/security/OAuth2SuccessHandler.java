package com.G7.CTBS.config.security;

import com.G7.CTBS.dto.UserCreateRequest;
import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.UserRepository;
import com.G7.CTBS.service.AuthenticationService;
import com.G7.CTBS.service.EmailService;
import com.G7.CTBS.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    UserRepository userRepository;
    AuthenticationService authenticationService;
    UserService userService;
    EmailService emailService;

    // Email làm Admin cứng
    private static final String ADMIN_EMAIL = "taikhoan.admin.cuaban@gmail.com";

    // Hàm sinh số điện thoại giả hợp lệ (Bắt đầu bằng 09 + 8 số ngẫu nhiên) để tránh lỗi trùng lặp DB
    private String generateRandomPhone() {
        return "09" + String.format("%08d", new Random().nextInt(100000000));
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String firstName = "";
        String lastName = "";

        if (name != null && name.contains(" ")) {
            firstName = name.substring(0, name.lastIndexOf(" "));
            lastName = name.substring(name.lastIndexOf(" ") + 1);
        } else {
            firstName = name != null ? name : email;
            lastName = "";
        }

        Optional<User> account = userRepository.findByUserNameOrEmail(email, email);

        // ========================================================
        // LUỒNG ĐẶC QUYỀN DÀNH RIÊNG CHO ADMIN
        // ========================================================
        if (ADMIN_EMAIL.equalsIgnoreCase(email)) {
            User adminUser;
            if (account.isEmpty()) {
                UserCreateRequest adminRequest = UserCreateRequest.builder()
                        .email(email)
                        .userName("SuperAdmin")
                        .firstName(firstName)
                        .lastName(lastName)
                        .password("Admin_Google@123")
                        .confirmPassword("")
                        .phone(generateRandomPhone()) // Sửa lỗi crash trùng phone
                        .gender("Khác")
                        .dob(LocalDate.of(2000, 1, 1))
                        .provider("GOOGLE")
                        .roleId(1L)
                        .build();
                adminUser = userService.create(adminRequest);
            } else {
                adminUser = account.get(); // Nếu đã tạo rồi thì lấy ra dùng luôn
            }

            // Sửa lỗi Token: Bắt buộc lấy UserName để tạo JWT
            String identifier = (adminUser.getUserName() != null && !adminUser.getUserName().trim().isEmpty())
                    ? adminUser.getUserName()
                    : adminUser.getEmail();

            String token = authenticationService.tokenGeneration(identifier);

            HttpSession session = req.getSession(false);
            if (session != null) session.invalidate();
            org.springframework.security.core.context.SecurityContextHolder.clearContext();

            res.sendRedirect("/?token=" + token + "&username=" + URLEncoder.encode(adminUser.getUserName(), StandardCharsets.UTF_8));
            return;
        }

        // ========================================================
        // LUỒNG CHO NGƯỜI DÙNG BÌNH THƯỜNG
        // ========================================================
        if (account.isEmpty()) {
            UserCreateRequest pendingUser = UserCreateRequest.builder()
                    .email(email)
                    .userName(email.split("@")[0] + "_" + new Random().nextInt(1000)) // Tránh lỗi email dài quá 50 kí tự
                    .firstName(firstName)
                    .lastName(lastName)
                    .password("Google_Auth_Default@123")
                    .confirmPassword("")
                    .phone(generateRandomPhone()) // Sửa lỗi crash trùng phone
                    .gender("Khác")
                    .dob(LocalDate.of(2000, 1, 1))
                    .provider("GOOGLE")
                    .roleId(2L)
                    .build();

            HttpSession session = req.getSession(true);
            session.setAttribute("PENDING_USER_DATA", pendingUser);

            String otp = String.valueOf(new Random().nextInt(899999) + 100000);
            session.setAttribute("OTP_CODE", otp);
            session.setAttribute("OTP_TIME", System.currentTimeMillis());

            try {
                emailService.sendOtpEmail(email, otp);
            } catch (Exception e) {
                System.err.println("Lỗi gửi mail: " + e.getMessage());
            }

            res.sendRedirect("/verify-otp");
        } else {
            User user = account.get();
            String identifier = (user.getUserName() != null && !user.getUserName().trim().isEmpty())
                    ? user.getUserName()
                    : user.getEmail();

            String token = authenticationService.tokenGeneration(identifier);
            String rawName = user.getUserName() != null ? user.getUserName() : user.getEmail();
            if (rawName == null || rawName.trim().isEmpty()) rawName = "Google User";
            String encodedName = URLEncoder.encode(rawName, StandardCharsets.UTF_8.toString());

            HttpSession session = req.getSession(false);
            if (session != null) session.invalidate();
            org.springframework.security.core.context.SecurityContextHolder.clearContext();

            res.sendRedirect("/?token=" + token + "&username=" + encodedName);
        }
    }
}