package org.proj.config;

import org.proj.entity.RoleEntity;
import org.proj.repository.RoleRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initRoles(RoleRepo roleRepo) {
        return args -> {
            List<String> roles = List.of("admin", "patient", "doctor", "receptionist");
            for (String roleName : roles) {
                if (roleRepo.findByRoleName(roleName).isEmpty()) {
                    RoleEntity role = RoleEntity.builder()
                            .roleName(roleName)
                            .isActive(true)
                            .build();
                    roleRepo.save(role);
                }
            }
        };
    }
    
}
