package com.G7.CTBS.controller;

import com.G7.CTBS.entity.Role;
import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.RoleRepository;
import com.G7.CTBS.repository.UserRepository;
import com.G7.CTBS.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<Map<String, Object>> safeUsers = userRepository.findAll().stream().map(user -> {
            String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "N/A";

            // Dùng HashMap thay vì Map.of() để có thể thêm bao nhiêu thuộc tính tùy thích
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("userId", user.getUserId());
            map.put("userName", user.getUserName());
            map.put("email", user.getEmail());
            map.put("phone", user.getPhone() != null ? user.getPhone() : "");
            map.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            map.put("lastName", user.getLastName() != null ? user.getLastName() : "");
            map.put("gender", user.getGender() != null ? user.getGender() : "");
            map.put("dob", user.getDob() != null ? user.getDob() : "");
            map.put("provider", user.getProvider() != null ? user.getProvider() : "LOCAL");
            map.put("role", Map.of("roleName", roleName));

            // ĐÃ FIX: Chuyển đổi Ngày Giờ (LocalDateTime) sang chuỗi văn bản cực đẹp
            if (user.getCreatedAt() != null) {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
                map.put("createdAt", user.getCreatedAt().format(formatter));
            } else {
                map.put("createdAt", "N/A");
            }

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(safeUsers);
    }

    // TRẢ VỀ ĐỐI TƯỢNG MAP CHỨA errorField VÀ message ĐỂ BẮT LỖI TẠI FRONTEND
    private Map<String, String> validateUserData(Map<String, String> data, Long excludeUserId) {
        String phone = data.get("phone");
        if (phone == null || !phone.matches("^(03|09)\\d{8}$")) {
            return Map.of("errorField", "phone", "message", "Phone number must be 10 digits and start with 03 or 09.");
        }

        if (excludeUserId == null && userRepository.existsByPhone(phone)) {
            return Map.of("errorField", "phone", "message", "This phone number is already registered.");
        } else if (excludeUserId != null && userRepository.existsByPhoneAndUserIdNot(phone, excludeUserId)) {
            return Map.of("errorField", "phone", "message", "This phone number is used by another account.");
        }

        String dobStr = data.get("dob");
        if (dobStr == null || dobStr.trim().isEmpty()) {
            return Map.of("errorField", "dob", "message", "Date of birth is required.");
        }

        LocalDate dob = LocalDate.parse(dobStr);
        if (dob.isAfter(LocalDate.now())) {
            return Map.of("errorField", "dob", "message", "Date of birth cannot be in the future.");
        }
        if (dob.getYear() < 1900 || dob.getYear() > 9999) {
            return Map.of("errorField", "dob", "message", "Invalid birth year.");
        }

        return null;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> data) {
        if (userRepository.existsByuserName(data.get("userName")))
            return ResponseEntity.badRequest().body(Map.of("errorField", "userName", "message", "Username already exists."));
        if (userRepository.existsByEmail(data.get("email")))
            return ResponseEntity.badRequest().body(Map.of("errorField", "email", "message", "Email already exists."));

        Map<String, String> validationError = validateUserData(data, null);
        if (validationError != null) return ResponseEntity.badRequest().body(validationError);

        User user = new User();
        user.setUserName(data.get("userName"));
        user.setEmail(data.get("email"));
        user.setFirstName(data.get("firstName"));
        user.setLastName(data.get("lastName"));
        user.setPhone(data.get("phone"));
        user.setGender(data.get("gender"));
        user.setDob(LocalDate.parse(data.get("dob")));
        user.setPassword(passwordEncoder.encode(data.get("password")));
        user.setProvider("LOCAL");
        user.setCreatedAt(LocalDateTime.now());

        Role userRole = roleRepository.findById(2L).orElse(null);
        user.setRole(userRole);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User created successfully!"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> data) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found!"));

        Map<String, String> validationError = validateUserData(data, id);
        if (validationError != null) return ResponseEntity.badRequest().body(validationError);

        user.setFirstName(data.get("firstName"));
        user.setLastName(data.get("lastName"));
        user.setPhone(data.get("phone"));
        user.setGender(data.get("gender"));
        user.setDob(LocalDate.parse(data.get("dob")));

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User updated successfully!"));
    }

    @PutMapping("/{id}/toggle-role")
    public ResponseEntity<?> toggleUserRole(@PathVariable Long id, Principal principal) {
        User targetUser = userRepository.findById(id).orElse(null);
        if (targetUser == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found!"));

        if (targetUser.getUserName().equals(principal.getName()) || targetUser.getEmail().equals(principal.getName())) {
            return ResponseEntity.status(403).body(Map.of("message", "System denied: You cannot change your own role!"));
        }

        boolean isCurrentlyAdmin = targetUser.getRole().getRoleName().toUpperCase().contains("ADMIN");
        Long newRoleId = isCurrentlyAdmin ? 2L : 1L;
        Role newRole = roleRepository.findById(newRoleId).orElse(null);
        targetUser.setRole(newRole);
        userRepository.save(targetUser);

        if (isCurrentlyAdmin) {
            String subject = "CTBS Cinema - Role Update Notification";
            String msg = "Hello " + targetUser.getFirstName() + ",\n\nYour account has been demoted to a standard User role by the Administration.\n\nRegards,\nCTBS Cinema Team.";
            emailService.sendNotificationEmail(targetUser.getEmail(), subject, msg);
        }

        return ResponseEntity.ok(Map.of("message", "Role updated successfully for " + targetUser.getUserName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Principal principal) {
        User targetUser = userRepository.findById(id).orElse(null);
        if (targetUser != null && (targetUser.getUserName().equals(principal.getName()) || targetUser.getEmail().equals(principal.getName()))) {
            return ResponseEntity.status(403).body(Map.of("message", "System denied: You cannot delete your own account!"));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully!"));
    }
}