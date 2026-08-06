package org.proj.service;

import org.proj.entity.UserEntity;
import org.proj.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service("securityService")
public class SecurityService {

    @Autowired
    private UserRepo userRepository;

    public boolean isOwner(Authentication authentication, UUID requestedId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return false;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        if (email == null || email.isBlank()) {
            return false;
        }

        Optional<UserEntity> userOpt = userRepository.findById(requestedId);
        if (userOpt.isEmpty()) {
            return false;
        }

        return userOpt.get().getEmail().equalsIgnoreCase(email.trim());
    }
}
