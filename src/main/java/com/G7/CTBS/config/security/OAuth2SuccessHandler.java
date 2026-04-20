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
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String ADMIN_EMAIL = "taikhoan.admin.cuaban@gmail.com";
    private static final int MAX_PHONE_RETRIES = 30;

    UserRepository userRepository;
    AuthenticationService authenticationService;
    UserService userService;
    EmailService emailService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            res.sendRedirect("/login?error=oauth_email_missing");
            return;
        }

        String name = oAuth2User.getAttribute("name");
        String firstName = extractFirstName(name, email);
        String lastName = extractLastName(name);

        Optional<User> account = userRepository.findByEmail(email);

        if (ADMIN_EMAIL.equalsIgnoreCase(email)) {
            User adminUser;
            if (account.isEmpty()) {
                UserCreateRequest adminRequest = UserCreateRequest.builder()
                        .email(email)
                        .userName(buildUniqueUsername("SuperAdmin"))
                        .firstName(firstName)
                        .lastName(lastName)
                        .password("Admin_Google@123")
                        .confirmPassword("")
                        .phone(generateUniquePhone())
                        .gender("Khac")
                        .dob(LocalDate.of(2000, 1, 1))
                        .provider("GOOGLE")
                        .roleId(1L)
                        .build();
                adminUser = userService.create(adminRequest);
            } else {
                adminUser = account.get();
            }

            String token = authenticationService.tokenGeneration(resolveTokenIdentifier(adminUser));
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            org.springframework.security.core.context.SecurityContextHolder.clearContext();

            res.sendRedirect("/?token=" + token + "&username="
                    + URLEncoder.encode(resolveDisplayName(adminUser), StandardCharsets.UTF_8));
            return;
        }

        if (account.isEmpty()) {
            UserCreateRequest pendingUser = UserCreateRequest.builder()
                    .email(email)
                    .userName(buildUniqueUsername(extractUsernameBase(email)))
                    .firstName(firstName)
                    .lastName(lastName)
                    .password("Google_Auth_Default@123")
                    .confirmPassword("")
                    .phone(generateUniquePhone())
                    .gender("Khac")
                    .dob(LocalDate.of(2000, 1, 1))
                    .provider("GOOGLE")
                    .roleId(2L)
                    .build();

            HttpSession session = req.getSession(true);
            session.setAttribute("PENDING_USER_DATA", pendingUser);

            String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
            session.setAttribute("OTP_CODE", otp);
            session.setAttribute("OTP_TIME", System.currentTimeMillis());

            try {
                emailService.sendOtpEmail(email, otp);
            } catch (Exception ignored) {
                // Keep flow unchanged: frontend will still land on OTP page.
            }

            res.sendRedirect("/verify-otp");
            return;
        }

        User user = account.get();
        String token = authenticationService.tokenGeneration(resolveTokenIdentifier(user));

        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        res.sendRedirect("/?token=" + token + "&username="
                + URLEncoder.encode(resolveDisplayName(user), StandardCharsets.UTF_8));
    }

    private String extractFirstName(String fullName, String fallbackEmail) {
        if (fullName == null || fullName.isBlank()) {
            return extractUsernameBase(fallbackEmail);
        }
        int lastSpace = fullName.lastIndexOf(' ');
        if (lastSpace <= 0) {
            return fullName;
        }
        return fullName.substring(0, lastSpace);
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        int lastSpace = fullName.lastIndexOf(' ');
        if (lastSpace <= 0 || lastSpace == fullName.length() - 1) {
            return "";
        }
        return fullName.substring(lastSpace + 1);
    }

    private String extractUsernameBase(String email) {
        if (email == null || email.isBlank()) {
            return "googleuser";
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String buildUniqueUsername(String rawBase) {
        String base = rawBase == null ? "googleuser" : rawBase.trim();
        base = base.replaceAll("[^A-Za-z0-9._-]", "");

        if (base.isBlank()) {
            base = "googleuser";
        }

        if (base.length() < 6) {
            String padded = base + "user123";
            base = padded.substring(0, 6);
        }

        String root = base.length() > 42 ? base.substring(0, 42) : base;
        String candidate = root;
        int suffix = 1;

        while (userRepository.existsByuserName(candidate)) {
            String suffixText = String.valueOf(suffix++);
            int maxRootLength = Math.max(1, 50 - suffixText.length());
            String adjustedRoot = root.length() > maxRootLength
                    ? root.substring(0, maxRootLength)
                    : root;
            candidate = adjustedRoot + suffixText;
        }

        return candidate;
    }

    private String generateUniquePhone() {
        for (int i = 0; i < MAX_PHONE_RETRIES; i++) {
            String phone = generateRandomPhone();
            if (!userRepository.existsByPhone(phone)) {
                return phone;
            }
        }

        long fallback = Math.floorMod(System.nanoTime(), 100000000L);
        return "03" + String.format("%08d", fallback);
    }

    private String generateRandomPhone() {
        return "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100000000));
    }

    private String resolveTokenIdentifier(User user) {
        if (user.getUserName() != null && !user.getUserName().trim().isEmpty()) {
            return user.getUserName();
        }
        return user.getEmail();
    }

    private String resolveDisplayName(User user) {
        String rawName = user.getUserName();
        if (rawName == null || rawName.trim().isEmpty()) {
            return extractUsernameBase(user.getEmail());
        }
        return rawName;
    }
}
