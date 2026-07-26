package org.proj.service;

import org.proj.entity.AccountEntity;
import org.proj.repository.AccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("securityService")
public class SecurityService {

    @Autowired
    private AccountRepo accountRepository;

    public boolean isOwner(Authentication authentication, Long requestedId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return false;
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        if (email == null || email.isBlank()) {
            return false;
        }

        Optional<AccountEntity> accountOpt = accountRepository.findById(requestedId);
        if (accountOpt.isEmpty()) {
            return false;
        }

        return accountOpt.get().getEmail().equalsIgnoreCase(email.trim());
    }
}
