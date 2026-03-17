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
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**", "/banners/**", "/trailers/**").permitAll()
                        .requestMatchers("/login", "/register", "/verify-otp", "/api/auth/**").permitAll()
                        .requestMatchers("/", "/index.html", "/movies", "/detail", "/about", "/movies/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        
                        // KHÓA TRANG ADMIN: Chỉ Role_Admin mới được vào
                        .requestMatchers("/admin/**", "/api/admin/**").hasAuthority("ROLE_Admin")
                        
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