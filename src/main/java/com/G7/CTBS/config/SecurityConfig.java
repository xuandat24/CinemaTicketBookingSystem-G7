package com.G7.CTBS.config;

import com.G7.CTBS.config.security.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // 1. CÁC TÀI NGUYÊN TĨNH (Ai cũng được tải)
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**", "/banners/**", "/trailers/**").permitAll()

                        // 2. CÁC ĐƯỜNG DẪN PUBLIC BẮT BUỘC
                        .requestMatchers("/login", "/register", "/verify-otp", "/api/auth/**").permitAll()
                        .requestMatchers("/", "/index", "/index.html", "/about").permitAll()
                        .requestMatchers("/movies", "/movies/**", "/detail").permitAll()
                        .requestMatchers("/showtimes", "/api/showtimes/**", "/showtimes/**").permitAll() // Đã khôi phục để không lỗi Lịch chiếu
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // 3. KHÓA TRANG HTML ADMIN: Cho phép tải HTML để admin-auth.js kiểm tra Token (Không chặn ở đây)
                        .requestMatchers("/admin/**").permitAll()

                        // 4. KHÓA CHẶT API ADMIN (BẢO VỆ DỮ LIỆU): Dùng hasAnyAuthority để tránh lỗi viết hoa/viết thường
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_Admin")

                        // Các yêu cầu khác (như /profile) phải đăng nhập
                        .anyRequest().authenticated()
                )

                // Gắn Jwt Filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // ==========================================================
                // BỘ XỬ LÝ NGOẠI LỆ THÔNG MINH (Kế thừa từ bản cập nhật mới)
                // ==========================================================
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String requestURI = request.getRequestURI();
                            System.out.println(">> [SECURITY BLOCK] Truy cập bị từ chối tại: " + requestURI);

                            // "Tàng hình" hệ thống Admin: Nếu không có quyền, báo Not Found (404) thay vì Forbidden (403)
                            if (requestURI.startsWith("/admin") || requestURI.startsWith("/api/admin")) {
                                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
                            } else {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                            }
                        })
                )

                // Cấu hình OAuth2
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(oAuth2SuccessHandler)
                )

                // ==========================================================
                // CẤU HÌNH LOGOUT TỐI ƯU (Kế thừa từ bản cập nhật mới)
                // ==========================================================
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("jwtToken", "JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                );

        return http.build();
    }
}