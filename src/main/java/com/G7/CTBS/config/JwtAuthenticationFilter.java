package com.G7.CTBS.config;

import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.UserRepository;
import com.G7.CTBS.service.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. TỐI ƯU HIỆU SUẤT: Bỏ qua Filter với các file tĩnh
        if (uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/img")
                || uri.startsWith("/fonts") || uri.startsWith("/banners") || uri.startsWith("/trailers")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        String authHeader = request.getHeader("Authorization");

        // 2. LẤY TOKEN
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwtToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 3. Nếu không có Token -> Khách Vãng Lai (Guest)
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 4. Giải mã Token
            String username = authenticationService.extractUsername(token);

            if (username != null) {

                // BẢO MẬT: Xóa Session cũ nếu phát hiện sự không trùng khớp
                if (SecurityContextHolder.getContext().getAuthentication() != null) {
                    String currentSessionUser = SecurityContextHolder.getContext().getAuthentication().getName();
                    if (!username.equals(currentSessionUser)) {
                        SecurityContextHolder.clearContext();
                    }
                }

                // 5. KIỂM TRA ROLE ĐỂ CẤP QUYỀN (ÁP DỤNG Ý TƯỞNG CỦA BẠN)
                if (SecurityContextHolder.getContext().getAuthentication() == null) {

                    // DÙNG JOIN FETCH ĐỂ BẮT BUỘC LẤY USER KÈM ROLE RÕ RÀNG
                    Optional<User> userOpt = userRepository.findByUsernameOrEmailWithRole(username);

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // LỚP BẢO VỆ KÉP: Kiểm tra xem User này có thực sự được gán Role không
                        if (user.getRole() != null && user.getRole().getRoleName() != null) {
                            String roleName = user.getRole().getRoleName();

                            // CHUẨN HÓA QUYỀN LỰC: Đảm bảo Security nhận diện đúng chuẩn "ROLE_ADMIN" hoặc "ROLE_USER"
                            String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;

                            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                                    new SimpleGrantedAuthority(authority)
                            );

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(username, null, authorities);

                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            // Chính thức cấp thẻ ra vào cho luồng này
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } else {
                            System.err.println(">> [CẢNH BÁO BẢO MẬT] Tài khoản " + username + " không có Role hợp lệ. Đã từ chối cấp quyền!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(">> [JWT FILTER] LỖI TOKEN: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 6. Cho phép đi tiếp
        filterChain.doFilter(request, response);
    }
}