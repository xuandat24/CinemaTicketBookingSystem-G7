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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/verify-otp", "/movie-seats", "/api/auth/**", "/api/showtimes/**", "/profile", "/booking/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        // Sử dụng biến đã được tiêm vào ở trên
                        .successHandler(oAuth2SuccessHandler)
                );

        return http.build();
    }
}
