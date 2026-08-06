package org.proj.config;

import org.proj.entity.RoleEntity;
import org.proj.repository.RoleRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepo roleRepo;

    public RoleInitializer(RoleRepo roleRepo) {
        this.roleRepo = roleRepo;
    }

    @Override
    public void run(String... args) {
        List<String> roles = List.of("admin", "doctor", "patient", "receptionist");
        for (String roleName : roles) {
            if (roleRepo.findByRoleName(roleName).isEmpty()) {
                roleRepo.save(RoleEntity.builder()
                        .roleName(roleName)
                        .isActive(true)
                        .build());
            }
        }
    }
}
