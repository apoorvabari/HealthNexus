package org.proj.controller;

import java.util.List;
import java.util.UUID;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.dto.UserFilterRequest;
import org.proj.dto.LogoutResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.dto.PageResponse;
import org.proj.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

import org.proj.entity.UserEntity;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/users")

public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = userService.register(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestParam(required = false) String userId) {
        return ResponseEntity.ok(userService.logout(userId));
    }

    @GetMapping("/profile")
    public ResponseEntity<RegisterResponse> getProfile(Authentication authentication) {
        UserEntity userDetails = (UserEntity) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getUserByEmail(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST') or (hasRole('PATIENT') and @securityService.isOwner(authentication, #id))")
    public ResponseEntity<RegisterResponse> getUserById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<PageResponse<RegisterResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllUsers(search, page, size));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<List<RegisterResponse>> filterUsers(
            @ModelAttribute UserFilterRequest request) {
        return ResponseEntity.ok(userService.filterUsers(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST') or (hasRole('PATIENT') and @securityService.isOwner(authentication, #id))")
    public ResponseEntity<RegisterResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<String> deleteUser(
            @PathVariable UUID id) {

        userService.deleteUser(id);

        return ResponseEntity.ok("User deleted successfully");
    }

    @PostMapping("/update-last-login")
    public ResponseEntity<String> updateLastLogin(Authentication authentication) {
        UserEntity userDetails = (UserEntity) authentication.getPrincipal();
        userService.updateLastLogin(userDetails.getUsername());
        return ResponseEntity.ok("Last login time updated successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody org.proj.dto.PasswordResetRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }

}
