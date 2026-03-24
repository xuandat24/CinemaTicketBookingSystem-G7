package com.G7.CTBS.controller;

import com.G7.CTBS.dto.UserDTO;
import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String MESSAGE = "message";
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 1. LẤY THÔNG TIN CỦA CHÍNH MÌNH DỰA VÀO TOKEN
    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(Map.of(MESSAGE, "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại!"));
        }

        String currentUserName = authentication.getName(); // Lấy tên mặc định

        // ========================================================
        // CHỐT CHẶN 2: ÉP LẤY EMAIL NẾU DÍNH SESSION GOOGLE
        // Nếu tên đang là 1 dãy số vô nghĩa của Google, tự động moi Email ra để tìm kiếm!
        // ========================================================
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User && oauth2User.getAttribute("email") != null) {
            currentUserName = oauth2User.getAttribute("email");
        }
        // ========================================================

        Optional<User> userOpt = userRepository.findByUserNameOrEmail(currentUserName, currentUserName);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            UserDTO userDTO = UserDTO.builder()
                    .userId(user.getUserId())
                    .roleId(user.getRole() != null ? user.getRole().getRoleId() : null)
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .userName(user.getUserName())
                    .phone(user.getPhone())
                    .email(user.getEmail())
                    .gender(user.getGender())
                    .dob(user.getDob())
                    .createdAt(user.getCreatedAt())
                    .build();

            return ResponseEntity.ok(userDTO);
        }

        return ResponseEntity.status(404).body(Map.of(MESSAGE, "Không tìm thấy dữ liệu người dùng!"));
    }
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserDTO updatedData, Authentication authentication) {
        String currentUserName = authentication.getName();
        Optional<User> userOpt = userRepository.findByUserNameOrEmail(currentUserName, currentUserName);

        if (userOpt.isPresent()) {
            User existingUser = userOpt.get();
            String newPhone = updatedData.getPhone().trim();
            String oldPhone = existingUser.getPhone().trim();

            // 1. CHỈ KIỂM TRA KHI NGƯỜI DÙNG CỐ TÌNH THAY ĐỔI SỐ ĐIỆN THOẠI
            // 2. TÌM XEM CÓ AI DÙNG SỐ NÀY MÀ KHÔNG PHẢI LÀ MÌNH KHÔNG
            if (!oldPhone.equals(newPhone) && userRepository.existsByPhoneAndUserIdNot(newPhone, existingUser.getUserId())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "errors", List.of(
                                Map.of(
                                        "field", "phone",
                                        "defaultMessage", "Số điện thoại này đã được tài khoản khác sử dụng!"
                                )
                        )
                ));
            }

            // 3. NẾU VƯỢ QUA ĐƯỢC THÌ CHO PHÉP LƯU
            existingUser.setGender(updatedData.getGender());
            existingUser.setFirstName(updatedData.getFirstName());
            existingUser.setLastName(updatedData.getLastName());
            existingUser.setDob(updatedData.getDob());
            existingUser.setPhone(newPhone);

            userRepository.save(existingUser);
            return ResponseEntity.ok(Map.of(MESSAGE, "Cập nhật thành công"));
        }
        return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Người dùng không tồn tại"));
    }

    // 3. XÓA TÀI KHOẢN
    @DeleteMapping("/delete-profile")
    public ResponseEntity<?> deleteProfile(Authentication authentication) {
        String currentUserName = authentication.getName();
        Optional<User> userOpt = userRepository.findByUserNameOrEmail(currentUserName, currentUserName);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userRepository.delete(user);
            return ResponseEntity.ok(Map.of(MESSAGE, "Xóa tài khoản thành công"));
        }
        return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Lỗi xóa tài khoản"));
    }
}