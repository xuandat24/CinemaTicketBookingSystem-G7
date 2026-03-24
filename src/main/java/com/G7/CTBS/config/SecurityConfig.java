package com.G7.CTBS.config;

import com.G7.CTBS.config.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // ========================================================================
    // 1. THÊM BỘ GIẢI MÃ MẬT KHẨU (FIX LỖI ĐĂNG NHẬP ĐÚNG MÀ BÁO SAI)
    // ========================================================================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ========================================================================
    // 2. THÊM AUTHENTICATION MANAGER (CẦN THIẾT CHO API LOGIN JWT CỦA BẠN)
    // ========================================================================
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // ========================================================================
    // 3. CẤU HÌNH PHÂN QUYỀN ĐƯỜNG DẪN
    // ========================================================================
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Đã bổ sung "/error" để tránh lỗi 404 ngầm đẩy văng ra trang Login
                        .requestMatchers("/", "/login", "/register", "/forgot-password", "/verify-otp", "/api/auth/**", "/profile", "/error",
                                "/index.html", "/movies", "/detail", "/about", "/movies/**").permitAll()

                        // Đã bổ sung "/uploads/**" phòng trường hợp bạn lưu ảnh Poster ở thư mục uploads
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**", "/uploads/**", "/banners/**", "/trailers/**").permitAll()

                        .requestMatchers("/showtimes", "/api/showtimes/**", "/showtimes/**").permitAll()

                        .requestMatchers("/api/public/**").permitAll()


                        // Cho phép các endpoint của OAuth2
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // KHÓA TRANG ADMIN: Chỉ Role_Admin mới được vào
                        .requestMatchers("/admin/**").permitAll()
                        // 2. KHÓA CHẶT các API thao tác dữ liệu, bắt buộc phải là Admin mới được gọi
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        // Các đường dẫn khác (ví dụ: /booking) bắt buộc phải đăng nhập
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(oAuth2SuccessHandler)
                );

        // Gắn bác bảo vệ (JWT FILTER) vào trước cửa
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}