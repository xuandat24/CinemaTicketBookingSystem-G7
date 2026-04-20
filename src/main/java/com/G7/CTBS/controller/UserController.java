package com.G7.CTBS.controller;

import com.G7.CTBS.dto.UserDTO;
import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.UserRepository;
import com.G7.CTBS.service.FileStorageService; // ĐÃ THÊM
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String MESSAGE = "message";
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService; // ĐÃ THÊM KHAI BÁO

    // ĐÃ THÊM Inject FileStorageService vào Constructor
    public UserController(UserRepository userRepository, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    // 1. LẤY THÔNG TIN CỦA CHÍNH MÌNH DỰA VÀO TOKEN
    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(Map.of(MESSAGE, "Invalid or expired token. Please log in again!"));
        }

        String currentUserName = authentication.getName(); // Lấy tên mặc định

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User && oauth2User.getAttribute("email") != null) {
            currentUserName = oauth2User.getAttribute("email");
        }

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
                    .avatar(user.getAvatar()) // BẮT BUỘC PHẢI THÊM DÒNG NÀY ĐỂ JS NHẬN ĐƯỢC ẢNH SAU KHI F5
                    .build();

            return ResponseEntity.ok(userDTO);
        }

        return ResponseEntity.status(404).body(Map.of(MESSAGE, "User data not found!"));
    }

    // 2. CẬP NHẬT PROFILE
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserDTO updatedData, Authentication authentication) {
        String currentUserName = authentication.getName();
        Optional<User> userOpt = userRepository.findByUserNameOrEmail(currentUserName, currentUserName);

        if (userOpt.isPresent()) {
            User existingUser = userOpt.get();
            String newPhone = updatedData.getPhone().trim();
            String oldPhone = existingUser.getPhone().trim();

            if (!oldPhone.equals(newPhone) && userRepository.existsByPhoneAndUserIdNot(newPhone, existingUser.getUserId())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "errors", List.of(
                                Map.of(
                                        "field", "phone",
                                        "defaultMessage", "This phone number is already in use by another account!"
                                )
                        )
                ));
            }

            // NẾU VƯỢ QUA ĐƯỢC THÌ CHO PHÉP LƯU
            existingUser.setGender(updatedData.getGender());
            existingUser.setFirstName(updatedData.getFirstName());
            existingUser.setLastName(updatedData.getLastName());
            existingUser.setDob(updatedData.getDob());
            existingUser.setPhone(newPhone);

            // ĐÃ THÊM: Lưu đường dẫn ảnh vào DB
            if (updatedData.getAvatar() != null) {
                existingUser.setAvatar(updatedData.getAvatar());
            }

            userRepository.save(existingUser);
            return ResponseEntity.ok(Map.of(MESSAGE, "Update successful"));
        }
        return ResponseEntity.badRequest().body(Map.of(MESSAGE, "User does not exist"));
    }

    // 3. XÓA TÀI KHOẢN
    @DeleteMapping("/delete-profile")
    public ResponseEntity<?> deleteProfile(Authentication authentication) {
        String currentUserName = authentication.getName();
        Optional<User> userOpt = userRepository.findByUserNameOrEmail(currentUserName, currentUserName);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userRepository.delete(user);
            return ResponseEntity.ok(Map.of(MESSAGE, "Account deleted successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Error deleting account"));
    }

    // 4. UPLOAD VÀ TỰ ĐỘNG CẬP NHẬT ẢNH ĐẠI DIỆN ĐỘC LẬP
    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(Map.of(MESSAGE, "Unauthorized"));
        }

        String currentUserName = authentication.getName();
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User && oauth2User.getAttribute("email") != null) {
            currentUserName = oauth2User.getAttribute("email");
        }

        Optional<User> userOpt = userRepository.findByUserNameOrEmail(currentUserName, currentUserName);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 1. DỌN RÁC: Xóa file avatar cũ khỏi ổ cứng (nếu có và không phải link web ngoài)
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                fileStorageService.deleteFile(user.getAvatar());
            }

            // 2. LƯU FILE MỚI: Đặt tên file theo định dạng user-{id} cho an toàn
            String storedPath = fileStorageService.storeFile(file, "avatars", "user-" + user.getUserId());

            // 3. LƯU VÀO DB NGAY LẬP TỨC
            user.setAvatar(storedPath);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    MESSAGE, "Avatar updated successfully",
                    "imagePath", storedPath
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(MESSAGE, "User not found"));
    }
}