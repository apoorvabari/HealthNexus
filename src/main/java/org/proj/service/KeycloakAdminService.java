package org.proj.service;

import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.proj.dto.AccountRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class KeycloakAdminService {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String adminRealm;

    @Value("${keycloak.admin.username}")
    private String username;

    @Value("${keycloak.admin.password}")
    private String password;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.realm}")
    private String realm;

    private Keycloak getKeycloakInstance() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(adminRealm)
                .username(username)
                .password(password)
                .clientId(clientId)
                .build();
    }

    public String createUserInKeycloak(AccountRequest request) {
        Keycloak keycloak = getKeycloakInstance();
        
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getEmail().trim());
        user.setEmail(request.getEmail().trim());
        user.setFirstName(request.getFirstName() != null ? request.getFirstName().trim() : null);
        user.setLastName(request.getLastName() != null ? request.getLastName().trim() : null);
        user.setEnabled(true);
        user.setEmailVerified(true);

        Response response = keycloak.realm(realm).users().create(user);
        if (response.getStatus() == 409) {
            throw new IllegalArgumentException("User with this email already exists in Keycloak.");
        } else if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user in Keycloak. Status code: " + response.getStatus());
        }

        String path = response.getLocation().getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);

        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getPassword());
            credential.setTemporary(false);

            keycloak.realm(realm).users().get(userId).resetPassword(credential);

            String roleName = request.getRole().toUpperCase().trim();
            RoleRepresentation roleRep = keycloak.realm(realm).roles().get(roleName).toRepresentation();
            keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(roleRep));

        } catch (Exception e) {
            try {
                keycloak.realm(realm).users().get(userId).remove();
            } catch (Exception rollbackException) {
                System.err.println("CRITICAL: Failed to rollback user creation in Keycloak: " + rollbackException.getMessage());
                rollbackException.printStackTrace();
            }
            throw new RuntimeException("Failed to assign password or role in Keycloak: " + e.getMessage(), e);
        }

        return userId;
    }

    public void deleteUserInKeycloak(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        try {
            Keycloak keycloak = getKeycloakInstance();
            keycloak.realm(realm).users().get(userId).remove();
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to delete user in Keycloak during rollback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void resetUserPassword(String email, String newPassword) {
        Keycloak keycloak = getKeycloakInstance();
        java.util.List<UserRepresentation> users = keycloak.realm(realm).users().search(email.trim(), true);
        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("User with email " + email + " does not exist in Keycloak.");
        }
        String userId = users.get(0).getId();

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        keycloak.realm(realm).users().get(userId).resetPassword(credential);
    }

    public void updateUserInKeycloak(String currentEmail, String newEmail, String firstName, String lastName) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            java.util.List<UserRepresentation> users = keycloak.realm(realm).users().search(currentEmail.trim(), true);
            if (users == null || users.isEmpty()) {
                return;
            }
            UserRepresentation user = users.get(0);
            
            boolean updated = false;
            if (newEmail != null && !newEmail.isBlank() && !newEmail.equalsIgnoreCase(currentEmail)) {
                user.setEmail(newEmail.trim());
                user.setUsername(newEmail.trim());
                updated = true;
            }
            if (firstName != null && !firstName.isBlank()) {
                user.setFirstName(firstName.trim());
                updated = true;
            }
            if (lastName != null && !lastName.isBlank()) {
                user.setLastName(lastName.trim());
                updated = true;
            }

            if (updated) {
                keycloak.realm(realm).users().get(user.getId()).update(user);
            }
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to update user in Keycloak: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
