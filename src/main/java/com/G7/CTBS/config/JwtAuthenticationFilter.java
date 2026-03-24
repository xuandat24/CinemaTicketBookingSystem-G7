package com.G7.CTBS.config;

import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.UserRepository;
import com.G7.CTBS.service.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository; // THÊM DÒNG NÀY ĐỂ TÌM USER

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String username = authenticationService.extractUsername(token);

            if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){

                // 1. TÌM USER TRONG DATABASE ĐỂ LẤY ROLE
                Optional<User> userOpt = userRepository.findByUserNameOrEmail(username, username);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    // 2. LẤY TÊN QUYỀN TỪ DATABASE (VD: "ROLE_ADMIN" hoặc "ROLE_USER")
                    String roleName = user.getRole().getRoleName();
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleName));

                    // 3. NẠP QUYỀN VÀO SPRING SECURITY THAY VÌ EMPTY LIST NHƯ CŨ
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    authorities
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (ParseException e) {
            System.out.println("JWT token parse error");
        }

        filterChain.doFilter(request, response);
    }
}