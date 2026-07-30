package org.proj.controller;

import java.util.List;
import java.util.UUID;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.LogoutResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "http://localhost:8082")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerAccount(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = accountService.register(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(accountService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestParam(required = false) String userId) {
        return ResponseEntity.ok(accountService.logout(userId));
    }

    @GetMapping("/profile")
    public ResponseEntity<RegisterResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return ResponseEntity.ok(accountService.getAccountByEmail(email));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST') or (hasRole('PATIENT') and @securityService.isOwner(authentication, #id))")
    public ResponseEntity<RegisterResponse> getAccountById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<List<RegisterResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<List<RegisterResponse>> filterAccounts(
            @ModelAttribute AccountFilterRequest request) {
        return ResponseEntity.ok(accountService.filterAccounts(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST') or (hasRole('PATIENT') and @securityService.isOwner(authentication, #id))")
    public ResponseEntity<RegisterResponse> updateAccount(
            @PathVariable UUID id,
            @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<String> deleteAccount(
            @PathVariable UUID id) {

        accountService.deleteAccount(id);

        return ResponseEntity.ok("Account deleted successfully");
    }

    @PostMapping("/update-last-login")
    public ResponseEntity<String> updateLastLogin(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        String keycloakUserId = jwt.getSubject();
        System.out.println("DEBUG: Extracted email for login update is: '" + email + "'");
        System.out.println("DEBUG: Extracted Keycloak User ID is: '" + keycloakUserId + "'");
        accountService.updateLastLogin(email, keycloakUserId);
        return ResponseEntity.ok("Last login time updated successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody org.proj.dto.PasswordResetRequest request) {
        accountService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }

}
