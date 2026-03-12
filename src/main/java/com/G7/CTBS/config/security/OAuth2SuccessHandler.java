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

        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        if (firstName == null) firstName = oAuth2User.getAttribute("name");
        if (firstName == null) firstName = "Google";
        if (lastName == null) lastName = "User";

        Optional<User> account = userRepository.findByEmail(email);

        if (account.isEmpty()) {
            // Bổ sung đầy đủ các trường bắt buộc để không lỗi Database sau này
            UserCreateRequest pendingUser = UserCreateRequest.builder()
                    .email(email)
                    .userName(email) // Dùng email làm username mặc định
                    .firstName(firstName)
                    .lastName(lastName)
                    .password("Google_Auth_Default@123") // Mật khẩu tạm để pass validation
                    .phone("0000000000") // Giá trị mặc định
                    .roleId(2L)
                    .build();

            HttpSession session = req.getSession(true);
            session.setAttribute("PENDING_USER_DATA", pendingUser);

            String otp = String.valueOf(new Random().nextInt(899999) + 100000);
            session.setAttribute("OTP_CODE", otp);
            session.setAttribute("OTP_TIME", System.currentTimeMillis()); // Bắt buộc phải có cái này

            try {
                emailService.sendOtpEmail(email, otp);
                System.out.println("Session ID khi tạo: " + session.getId());
                System.out.println("Mã OTP đã gửi: " + otp);
            } catch (Exception e) {
                System.err.println("Lỗi gửi mail: " + e.getMessage());
            }

            res.sendRedirect("http://localhost:8080/verify-otp");
        } else {
            // Xử lý trường hợp đã có tài khoản (Đăng nhập thẳng)
            User user = account.get();
            String token = authenticationService.tokenGeneration(user.getUserName());
            res.sendRedirect("http://localhost:8080/?token=" + token);
        }
    }
}