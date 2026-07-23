package org.proj.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.dto.LogoutResponse;
import org.proj.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "http://localhost:8081")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<AccountResponse> registerAccount(
            @Valid @RequestBody AccountRequest request) {

        AccountResponse response = accountService.register(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginRequest request) {

        return ResponseEntity.ok(accountService.login(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody @Valid LoginRequest.PasswordResetRequest request) {
        accountService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestParam(required = false) String userId) {
        return ResponseEntity.ok(accountService.logout(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/filter")
    public ResponseEntity<List<AccountResponse>> filterAccounts(
            @ModelAttribute AccountFilterRequest request) {
        return ResponseEntity.ok(accountService.filterAccounts(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @RequestBody AccountRequest request) {

        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAccount(
            @PathVariable Long id) {

        accountService.deleteAccount(id);

        return ResponseEntity.ok("Account deleted successfully");
    }

}
