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
                        
                        // Đã mở rộng để bắt các lỗi gõ sai đuôi .html
                        .requestMatchers("/movies", "/movies/**", "/detail", "/detail.html", "/detail/**").permitAll()
                        
                        .requestMatchers("/showtimes", "/api/showtimes/**", "/showtimes/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        
                        // 3. KHÓA TRANG HTML (Giao việc chặn cho auth.js)
                        .requestMatchers("/admin/**").permitAll()
                        
                        // BỔ SUNG QUAN TRỌNG: Cho phép tải giao diện User và mở khóa /error để tránh bẫy 404
                        .requestMatchers("/profile", "/profile/**", "/booking/history", "/error").permitAll()
                        
                        // PAYMENT ENDPOINTS: Cho phép truy cập, sẽ validate JWT bên trong controller
                        .requestMatchers("/api/payment/**").permitAll()
                        
                        // 4. KHÓA CHẶT API BẰNG ROLE (BẢO VỆ DỮ LIỆU)
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_Admin")
                        
                        // Các yêu cầu API khác (như /api/users/my-profile, /api/booking/confirm) phải có Token
                        .anyRequest().authenticated()
                )

                // Gắn Jwt Filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // ==========================================================
                // BỘ XỬ LÝ NGOẠI LỆ THÔNG MINH (Kế thừa từ bản cập nhật mới)
                // ==========================================================
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestURI = request.getRequestURI();
                            if (requestURI.startsWith("/api/")) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                            } else {
                                response.sendRedirect("/login");
                            }
                        })
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
