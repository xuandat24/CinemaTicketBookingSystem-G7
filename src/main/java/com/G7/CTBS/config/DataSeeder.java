package com.G7.CTBS.config;

import com.G7.CTBS.entity.Role;
import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.RoleRepository;
import com.G7.CTBS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // 1. Khởi tạo Role nếu chưa có
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setRoleName("ROLE_ADMIN");

            Role userRole = new Role();
            userRole.setRoleName("ROLE_USER");

            roleRepository.saveAll(List.of(adminRole, userRole));
            log.info("✅ Created Roles: ROLE_ADMIN, ROLE_USER");
        }

        // 2. Lấy Role Admin từ DB (Dùng findByName để an toàn hơn dùng ID cứng)
        Role adminRole = roleRepository.findAll().stream()
                .filter(r -> r.getRoleName().equals("ROLE_ADMIN"))
                .findFirst()
                .orElse(null);

        if (adminRole != null) {
            // TÀI KHOẢN ADMIN 1
            if (!userRepository.existsByuserName("fujiokaharuhi")) {
                User admin1 = User.builder()
                        .userName("fujiokaharuhi")
                        .email("admin1@ctbs.com")
                        .password(passwordEncoder.encode("321yeuem:D@!"))
                        .firstName("System")
                        .lastName("Admin 1")
                        .phone("0999999991")
                        .gender("Nam")
                        .dob(LocalDate.of(2000, 1, 1))
                        .provider("LOCAL") // XÁC ĐỊNH LÀ TÀI KHOẢN THƯỜNG
                        .role(adminRole)
                        .build();
                userRepository.save(admin1);
                log.info(" Created Admin 1 (LOCAL)");
            }

            // TÀI KHOẢN ADMIN 2
            if (!userRepository.existsByuserName("321yeuem")) {
                User admin2 = User.builder()
                        .userName("321yeuem")
                        .email("admin2@ctbs.com")
                        .password(passwordEncoder.encode("321yeuem:D@!"))
                        .firstName("System")
                        .lastName("Admin 2")
                        .phone("0999999992")
                        .gender("Nữ")
                        .dob(LocalDate.of(2000, 2, 2))
                        .provider("LOCAL") // XÁC ĐỊNH LÀ TÀI KHOẢN THƯỜNG
                        .role(adminRole)
                        .build();
                userRepository.save(admin2);
                log.info(" Created Admin 2 (LOCAL)");
            }
        }
        // 3. Lấy Role User (Customer)
        Role userRole = roleRepository.findAll().stream()
                .filter(r -> r.getRoleName().equals("ROLE_USER"))
                .findFirst()
                .orElse(null);

        if (userRole != null) {
            // TÀI KHOẢN CUSTOMER
            if (!userRepository.existsByuserName("customer1")) {
                User customer = User.builder()
                        .userName("customer1")
                        .email("customer1@ctbs.com")
                        .password(passwordEncoder.encode("User@123"))
                        .firstName("Nguyen")
                        .lastName("Van A")
                        .phone("0888888888")
                        .gender("Nam")
                        .dob(LocalDate.of(2002, 5, 10))
                        .provider("LOCAL") // tài khoản thường
                        .role(userRole)
                        .build();
                userRepository.save(customer);
                log.info(" Created Customer 1 (LOCAL)");
            }
        }
    }
}