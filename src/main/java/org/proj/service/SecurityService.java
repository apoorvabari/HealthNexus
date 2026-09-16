package org.proj.service;

import org.proj.entity.UserEntity;
import org.proj.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("securityService")
public class SecurityService {

    @Autowired
    private UserRepo userRepository;

    public boolean isOwner(Authentication authentication, UUID requestedId) {
        if (authentication == null || requestedId == null) {
            return false;
        }

        if (authentication.getPrincipal() instanceof UserEntity) {
            UserEntity user = (UserEntity) authentication.getPrincipal();
            return requestedId.equals(user.getId());
        }

        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            if (email == null || email.isBlank()) {
                return false;
            }
            return userRepository.findById(requestedId)
                    .map(u -> u.getEmail().equalsIgnoreCase(email.trim()))
                    .orElse(false);
        }

        return false;
    }
}
