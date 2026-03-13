package com.G7.CTBS.config.security;

import com.G7.CTBS.dto.UserCreateRequest;
import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.UserRepository;
import com.G7.CTBS.service.EmailService;
import com.G7.CTBS.service.UserService;
import com.G7.CTBS.service.AuthenticationService;
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
    UserService accountService;
    EmailService emailService;

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

        if (account.isEmpty()) {
            // TRƯỜNG HỢP 1: CHƯA CÓ TÀI KHOẢN -> GỬI OTP XÁC THỰC
            UserCreateRequest pendingUser = UserCreateRequest.builder()
                    .email(email)
                    .userName(email) // Dùng email làm username mặc định
                    .firstName(firstName)
                    .lastName(lastName)
                    .password("Google_Auth_Default@123") // Mật khẩu tạm để pass validation
                    .confirmPassword("")
                    .phone("0000000000") // Giá trị mặc định
                    .gender("Others") // Mặc định khi đăng ký qua Google
                    .dob(LocalDate.of(2000, 1, 1))
                    .roleId(2L)
                    .build();

            HttpSession session = req.getSession(true);
            session.setAttribute("PENDING_USER_DATA", pendingUser);

            String otp = String.valueOf(new Random().nextInt(899999) + 100000);
            session.setAttribute("OTP_CODE", otp);
            session.setAttribute("OTP_TIME", System.currentTimeMillis());

            try {
                emailService.sendOtpEmail(email, otp);
                System.out.println("Session ID khi tạo: " + session.getId());
                System.out.println("Mã OTP đã gửi: " + otp);
            } catch (Exception e) {
                System.err.println("Lỗi gửi mail: " + e.getMessage());
            }

            res.sendRedirect("/verify-otp");
        } else {
            User user = account.get();
            String token = authenticationService.tokenGeneration(user.getUserName());
            String rawName = user.getUserName();

// 1. Đề phòng trường hợp Google không trả về tên
            if (rawName == null || rawName.isEmpty()) {
                rawName = "Google User";
            }

// 2. Mã hóa khoảng trắng và dấu tiếng Việt an toàn cho URL
            String encodedName = URLEncoder.encode(rawName, StandardCharsets.UTF_8.toString());

// 3. Gửi Redirect
            res.sendRedirect("/?token=" + token + "&username=" + encodedName);
        }
    }
}