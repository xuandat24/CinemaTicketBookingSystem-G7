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

    public User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + id));
    }

    // =========================================================
    // 2 HÀM MỚI ĐƯỢC BỔ SUNG ĐỂ HỖ TRỢ CHO BOOKING CONTROLLER
    // =========================================================
    public User findByUsernameOrEmail(String identifier) {
        // Tận dụng hàm findByUserNameOrEmail đã có sẵn trong UserRepository của bạn
        return repository.findByUserNameOrEmail(identifier, identifier).orElse(null);
    }

    public User findByUsername(String username) {
        // Tận dụng hàm findByuserName đã có sẵn trong UserRepository của bạn
        return repository.findByuserName(username).orElse(null);
    }
    // =========================================================

    public User create(UserCreateRequest req) {

        if (repository.existsByuserName(req.getUserName())) {
            throw new RuntimeException("Username is already taken");
        }
        if (repository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }
        if (repository.existsByPhone(req.getPhone())) {
            throw new RuntimeException("This phone number is already registered to another account!");
        }

        User user = new User();
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setUserName(req.getUserName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setCreatedAt(LocalDateTime.now());

        if (req.getProvider() != null && !req.getProvider().trim().isEmpty()) {
            user.setProvider(req.getProvider());
        } else {
            user.setProvider("LOCAL");
        }
        user.setGender(req.getGender());
        user.setDob(req.getDob());

        Integer roleId = req.getRoleId() != null ? req.getRoleId().intValue() : 2;
        Role role = roleService.findById(roleId);
        user.setRole(role);

        return repository.save(user);
    }

    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }

    public User registerNewUserFromGoogleData(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");

        User user = new User();
        user.setEmail(email);
        user.setFirstName((String) attributes.get("given_name"));
        user.setLastName((String) attributes.get("family_name"));

        user.setUserName(email.split("@")[0] + "_" + System.currentTimeMillis());
        user.setPassword(passwordEncoder.encode("GoogleAuth@123"));
        user.setPhone("0000000000");
        user.setCreatedAt(LocalDateTime.now());

        Role userRole = roleRepository.findById(2L).orElse(null);
        user.setRole(userRole);

        return repository.save(user);
    }

    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
    }
}