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
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setRoleName("ROLE_ADMIN");

            Role userRole = new Role();
            userRole.setRoleName("ROLE_USER");

            roleRepository.saveAll(List.of(adminRole, userRole));
            log.info("Created roles: ROLE_ADMIN, ROLE_USER");
        }

        Role adminRole = roleRepository.findAll().stream()
                .filter(r -> "ROLE_ADMIN".equals(r.getRoleName()))
                .findFirst()
                .orElse(null);

        if (adminRole == null) {
            log.warn("Skip user seed because ROLE_ADMIN does not exist.");
            return;
        }

        seedAdminAccount(adminRole, "admin1", "admin1@ctbs.com", "System", "Admin 1",
                "0999999991", "Nam", LocalDate.of(2000, 1, 1));

        seedAdminAccount(adminRole, "admin2", "admin2@ctbs.com", "System", "Admin 2",
                "0999999992", "Nu", LocalDate.of(2000, 2, 2));
    }

    private void seedAdminAccount(Role adminRole,
                                  String userName,
                                  String email,
                                  String firstName,
                                  String lastName,
                                  String phone,
                                  String gender,
                                  LocalDate dob) {
        Optional<User> byEmail = userRepository.findByEmail(email);
        Optional<User> byUserName = userRepository.findByuserName(userName);

        if (byEmail.isPresent() && byUserName.isPresent()
                && !byEmail.get().getUserId().equals(byUserName.get().getUserId())) {
            log.warn("Skip seeding {} because username and email belong to different users.", userName);
            return;
        }

        User existingUser = byEmail.orElseGet(() -> byUserName.orElse(null));
        if (existingUser != null) {
            boolean updated = false;

            if (existingUser.getRole() == null
                    || existingUser.getRole().getRoleName() == null
                    || !adminRole.getRoleName().equals(existingUser.getRole().getRoleName())) {
                existingUser.setRole(adminRole);
                updated = true;
            }

            if (existingUser.getProvider() == null || existingUser.getProvider().isBlank()) {
                existingUser.setProvider("LOCAL");
                updated = true;
            }

            if (updated) {
                userRepository.save(existingUser);
                log.info("Updated existing user {} to admin defaults.", email);
            } else {
                log.info("Skip seeding {} because account already exists.", email);
            }
            return;
        }

        if (userRepository.existsByPhone(phone)) {
            log.warn("Skip seeding {} because phone {} is already used.", email, phone);
            return;
        }

        User adminUser = User.builder()
                .userName(userName)
                .email(email)
                .password(passwordEncoder.encode("Admin@123"))
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .gender(gender)
                .dob(dob)
                .provider("LOCAL")
                .role(adminRole)
                .build();

        userRepository.save(adminUser);
        log.info("Created admin account {}", email);
    }
}
