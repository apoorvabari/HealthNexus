package org.proj.controller;

import java.util.List;

import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.LogoutResponse;
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
    public ResponseEntity<AccountResponse> registerAccount(
            @Valid @RequestBody AccountRequest request) {

        AccountResponse response = accountService.register(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestParam(required = false) String userId) {
        return ResponseEntity.ok(accountService.logout(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST') or (hasRole('PATIENT') and @securityService.isOwner(authentication, #id))")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<List<AccountResponse>> filterAccounts(
            @ModelAttribute AccountFilterRequest request) {
        return ResponseEntity.ok(accountService.filterAccounts(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST') or (hasRole('PATIENT') and @securityService.isOwner(authentication, #id))")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @RequestBody AccountRequest request) {

        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<String> deleteAccount(
            @PathVariable Long id) {

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

}
