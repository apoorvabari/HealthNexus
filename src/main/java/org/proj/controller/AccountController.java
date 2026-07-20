package org.proj.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
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
    public ResponseEntity<?> filterAccounts(
            @RequestParam Map<String, String> allParams) {

        Set<String> allowedParams = Set.of("id", "firstName", "email", "phoneNumber", "status");

        // 1. Validate keys
        for (String param : allParams.keySet()) {
            if (!allowedParams.contains(param)) {
                throw new IllegalArgumentException("Unknown query parameter: " + param);
            }
        }

        // 2. Extract and manually convert values
        Long id = allParams.containsKey("id") ? Long.parseLong(allParams.get("id")) : null;
        String firstName = allParams.get("firstName");
        String email = allParams.get("email");
        String phoneNumber = allParams.get("phoneNumber");
        String status = allParams.get("status");

        List<AccountResponse> accounts =
                accountService.filterAccounts(id, firstName, email, phoneNumber, status);

        if (accounts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No accounts found."));
        }

        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequest request) {

        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAccount(
            @PathVariable Long id) {

        accountService.deleteAccount(id);

        return ResponseEntity.ok("Account deleted successfully");
    }

}
