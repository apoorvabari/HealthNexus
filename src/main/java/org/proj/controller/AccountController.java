package org.proj.controller;

import java.util.List;
import java.util.UUID;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.springframework.security.core.Authentication;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.LogoutResponse;
import org.proj.dto.PasswordResetRequest;
import org.proj.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
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

        LoginResponse response = accountService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestParam(required = false) String userId) {
        return ResponseEntity.ok(accountService.logout(userId));
    }

    @GetMapping("/profile")
    public ResponseEntity<RegisterResponse> getProfile() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        return ResponseEntity.ok(accountService.getAccountByEmail(email));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'PATIENT', 'ADMIN')")
    public ResponseEntity<RegisterResponse> getAccountById(
            @PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(accountService.getAccountById(id, email));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<List<RegisterResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<List<RegisterResponse>> filterAccounts(
            @ModelAttribute AccountFilterRequest request) {
        return ResponseEntity.ok(accountService.filterAccounts(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'PATIENT', 'ADMIN')")
    public ResponseEntity<RegisterResponse> updateAccount(
            @PathVariable UUID id,
            @RequestBody RegisterRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(accountService.updateAccount(id, request, email));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<String> deleteAccount(
            @PathVariable UUID id) {

        accountService.deleteAccount(id);

        return ResponseEntity.ok("Account deleted successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        accountService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }

}
