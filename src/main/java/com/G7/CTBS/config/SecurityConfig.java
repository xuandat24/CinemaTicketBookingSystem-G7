package com.G7.CTBS.config;

import com.G7.CTBS.config.security.OAuth2SuccessHandler; // Import handler của bạn
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor // Tự động tạo Constructor để tiêm oAuth2SuccessHandler vào
public class SecurityConfig {

    // Khai báo biến Handler
    private final OAuth2SuccessHandler oAuth2SuccessHandler;


    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/verify-otp", "/movie-seats", "/api/auth/**", "/api/showtimes/**", "/profile", "/booking/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**", "/banners/**", "/trailers/**").permitAll()
                        .requestMatchers("/login", "/register", "/verify-otp", "/api/auth/**").permitAll()
                        .requestMatchers("/", "/index.html", "/movies", "/detail", "/about", "/movies/**").permitAll()
                        .requestMatchers("/showtimes", "/api/showtimes/**", "/showtimes/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String requestURI = request.getRequestURI();
                            if (requestURI.startsWith("/admin") || requestURI.startsWith("/api/admin")) {
                                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
                            } else {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                            }
                        })
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        // Sử dụng biến đã được tiêm vào ở trên
                        .successHandler(oAuth2SuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // Bắt tín hiệu từ form logout
                        .logoutSuccessUrl("/login?logout") // Chuyển về trang login
                        .deleteCookies("jwtToken", "JSESSIONID")
                        .invalidateHttpSession(true) // Xóa session của Spring
                        .clearAuthentication(true) // Xóa quyền trong Context
                );

        return http.build();
    }
}
