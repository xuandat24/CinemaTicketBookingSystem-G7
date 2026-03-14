package com.G7.CTBS.config;

import com.G7.CTBS.config.security.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    
    // BẮT BUỘC: Khai báo Filter mà chúng ta vừa sửa ở trên
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**", "/banners/**", "/trailers/**").permitAll()
                        .requestMatchers("/login", "/register", "/verify-otp", "/api/auth/**").permitAll()
                        .requestMatchers("/", "/index.html", "/movies.html", "/detail.html", "/about.html").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        
                        // KHÓA TRANG ADMIN: Chỉ Role_Admin mới được vào
                        .requestMatchers("/admin/**", "/api/admin/**").hasAuthority("ROLE_Admin")
                        
                        .anyRequest().authenticated()
                )
                // LỖI NẰM Ở ĐÂY: Cần chèn JWT Filter vào chuỗi bảo mật của Spring
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
                        .successHandler(oAuth2SuccessHandler)
                );
        
        return http.build();
    }
}