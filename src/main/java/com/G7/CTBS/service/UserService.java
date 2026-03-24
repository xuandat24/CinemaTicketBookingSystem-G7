package com.G7.CTBS.service;

import com.G7.CTBS.dto.UserCreateRequest;
import com.G7.CTBS.entity.Role;
import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.RoleRepository;
import com.G7.CTBS.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    @Autowired
    private UserRepository repository;
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleService roleService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    public List<User> findAll() {
        return repository.findAll();
    }

    public User findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Account với ID: " + id));
    }

    public User create(UserCreateRequest req) {

        if (repository.existsByuserName(req.getUserName())) {
            throw new RuntimeException("Tên người dùng đã được sử dụng");
        }

        if (repository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        if (repository.existsByPhone(req.getPhone())) {
            throw new RuntimeException("Số điện thoại này đã được đăng ký cho một tài khoản khác!");
        }
        User user = new User();

        // Lấy TOÀN BỘ dữ liệu thật do người dùng nhập từ req (Front-end gửi lên)
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setUserName(req.getUserName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword())); // Mã hóa mật khẩu
        user.setCreatedAt(LocalDateTime.now()); // Thời gian tạo
        // ==========================================
        // XỬ LÝ PROVIDER CHUẨN XÁC
        // ==========================================
        if (req.getProvider() != null && !req.getProvider().trim().isEmpty()) {
            user.setProvider(req.getProvider()); // Nếu là GOOGLE truyền sang
        } else {
            user.setProvider("LOCAL"); // Rỗng hoặc null mặc định là LOCAL
        }
        user.setGender(req.getGender());
        user.setDob(req.getDob());
        // Gán Role (Mặc định là 2)
        Integer roleId = req.getRoleId() != null ? req.getRoleId().intValue() : 2;

        // Gọi thẳng service (không cần orElseThrow ở đây nữa)
        Role role = roleService.findById(roleId);

        user.setRole(role);


        // BẮT BUỘC PHẢI CÓ DÒNG NÀY ĐỂ LƯU XUỐNG DATABASE
        return repository.save(user);
    }

    public void delete(Integer id) {
        findById(id);
        repository.deleteById(id);
    }
    // Trong UserService.java

    public User registerNewUserFromGoogleData(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");

        // Tạo đối tượng User thực thể để lưu vào DB
        User user = new User();
        user.setEmail(email);
        user.setFirstName((String) attributes.get("given_name"));
        user.setLastName((String) attributes.get("family_name"));

        // Tạo username unique
        user.setUserName(email.split("@")[0] + "_" + System.currentTimeMillis());
        user.setPassword(passwordEncoder.encode("GoogleAuth@123")); // Pass mặc định
        user.setPhone("0000000000"); // Mặc định để tránh lỗi database
        user.setCreatedAt(LocalDateTime.now());

        // Gán Role mặc định (ID = 2)
        Role userRole = roleRepository.findById(2L).orElse(null);
        user.setRole(userRole);

        return repository.save(user);
    }

    // THÊM HÀM NÀY VÀO CUỐI USER SERVICE
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
    }

}