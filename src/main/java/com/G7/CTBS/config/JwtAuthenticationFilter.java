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
        // Bỏ qua lọc với các file tĩnh để màn hình Console không bị rác
        if (uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/img") || uri.startsWith("/fonts")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        System.out.println("=========================================");
        System.out.println(">> [JWT FILTER] Đang kiểm tra URL: " + uri);
        
        String token = null;
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            System.out.println(">> [JWT FILTER] Đã tìm thấy Token trong Header (API Fetch)!");
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwtToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    System.out.println(">> [JWT FILTER] Đã tìm thấy Token trong Cookie (Chuyển trang HTML)!");
                    break;
                }
            }
        }
        
        if (token == null) {
            System.out.println(">> [JWT FILTER] KHÔNG CÓ TOKEN. Chuyển tiếp với quyền Khách (Guest).");
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String username = authenticationService.extractUsername(token);
            System.out.println(">> [JWT FILTER] Giải mã Token thành công. Username: " + username);
            
            // BẮT BỆNH SỐ 2: Nếu có Session rác từ trước, xóa ngay lập tức để ép nhận Token mới
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                System.out.println(">> [JWT FILTER] CẢNH BÁO: Đang có Session cũ của " + SecurityContextHolder.getContext().getAuthentication().getName() + ". Đã xóa!");
                SecurityContextHolder.clearContext();
            }
            
            if (username != null) {
                // Gọi hàm tìm user bằng username hoặc email từ database
                User user = userRepository.findByUsernameOrEmailWithRole(username).orElse(null);
                
                if (user != null && user.getRole() != null) {
                    String roleName = user.getRole().getRoleName(); // Nếu DB của bạn là name thì sửa thành getName()
                    String authority = "ROLE_" + roleName;
                    
                    System.out.println(">> [JWT FILTER] THÀNH CÔNG: Đã cấp quyền [" + authority + "] cho tài khoản [" + username + "]");
                    
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority(authority)
                    );
                    
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    System.out.println(">> [JWT FILTER] LỖI DB: Không tìm thấy User trong Database hoặc User chưa được gắn Role!");
                }
            }
        } catch (Exception e) {
            System.out.println(">> [JWT FILTER] LỖI TOKEN: " + e.getMessage());
        }
        
        System.out.println(">> [JWT FILTER] Chuyển request đi tiếp tới Controller...");
        filterChain.doFilter(request, response);
    }
}