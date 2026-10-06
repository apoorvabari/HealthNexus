package org.proj.config;

import org.proj.entity.RoleEntity;
import org.proj.entity.UserEntity;
import org.proj.repository.RoleRepo;
import org.proj.repository.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecureAdminInitializer {

    @Bean
    public CommandLineRunner initSecureAdmin(UserRepo userRepo, RoleRepo roleRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            boolean adminExists = userRepo.findAll().stream()
                    .anyMatch(user -> user.getRole() != null && "admin".equalsIgnoreCase(user.getRole().getRoleName()));

            if (!adminExists) {
                String adminEmail = System.getenv("ADMIN_EMAIL");
                String adminPassword = System.getenv("ADMIN_PASSWORD");
                String adminFirstName = System.getenv("ADMIN_FIRST_NAME");
                String adminLastName = System.getenv("ADMIN_LAST_NAME");

                if (adminEmail == null || adminPassword == null) {
                    System.out.println(">>> Skipping automatic admin creation: ADMIN_EMAIL or ADMIN_PASSWORD environment variables are not set.");
                    return;
                }

                RoleEntity adminRole = roleRepo.findByRoleName("admin")
                        .orElseGet(() -> roleRepo.save(RoleEntity.builder().roleName("admin").isActive(true).build()));

                UserEntity admin = UserEntity.builder()
                        .firstName(adminFirstName != null ? adminFirstName : "Platform")
                        .lastName(adminLastName != null ? adminLastName : "Admin")
                        .email(adminEmail.trim().toLowerCase())
                        .phoneNumber("0000000000")
                        .password(passwordEncoder.encode(adminPassword))
                        .role(adminRole)
                        .isActive(true)
                        .isEmailVerified(true)
                        .build();

                userRepo.save(admin);
                System.out.println(">>> Secure Platform Admin created successfully from environment variables.");
            }
        };
    }
}
