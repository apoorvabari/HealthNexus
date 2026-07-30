package org.proj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;
import java.util.Map;

@Configuration
public class KeycloakConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.Set<org.springframework.security.core.GrantedAuthority> authorities = new java.util.HashSet<>();

            // 1. Extract realm-level roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                if (roles != null) {
                    roles.forEach(
                            role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().trim())));
                }
            }

            System.out.println("DEBUG JWT CLAIMS: " + jwt.getClaims());

            // 2. Extract client-level roles from all registered clients in resource_access
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                resourceAccess.forEach((clientKey, clientValue) -> {
                    if (clientValue instanceof Map) {
                        Map<?, ?> clientMap = (Map<?, ?>) clientValue;
                        Object rolesObj = clientMap.get("roles");
                        if (rolesObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> clientRoles = (List<String>) rolesObj;
                            clientRoles.forEach(role -> authorities
                                    .add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().trim())));
                        }
                    }
                });
            }

            // 3. Extract roles directly from "roles" claim (if present as a flat list)
            Object directRoles = jwt.getClaim("roles");
            if (directRoles instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> rolesList = (List<String>) directRoles;
                rolesList.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().trim())));
            }

            // 4. Extract roles from "groups" or "group" claim
            Object groupsObj = jwt.getClaim("groups");
            if (groupsObj == null) {
                groupsObj = jwt.getClaim("group");
            }
            if (groupsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> groupsList = (List<String>) groupsObj;
                groupsList.forEach(group -> {
                    String role = group.startsWith("/") ? group.substring(1) : group;
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().trim()));
                });
            }

            System.out.println("DEBUG MAPPED AUTHORITIES: " + authorities);

            return authorities.stream().toList();
        });

        return converter;
    }
}
