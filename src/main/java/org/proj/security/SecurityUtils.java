package org.proj.security;

import org.proj.entity.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserEntity) {
            return (UserEntity) authentication.getPrincipal();
        }
        return null;
    }

    public static boolean isPatient() {
        UserEntity currentUser = getCurrentUser();
        return currentUser != null && currentUser.getRole() != null 
               && "PATIENT".equalsIgnoreCase(currentUser.getRole().getRoleName());
    }
}
