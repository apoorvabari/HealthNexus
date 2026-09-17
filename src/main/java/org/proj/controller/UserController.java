package org.proj.controller;

import java.util.List;
import java.util.UUID;

import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.dto.LogoutResponse;
import org.proj.dto.PageResponse;
import org.proj.dto.PasswordResetRequest;
import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.dto.UserFilterRequest;
import org.proj.entity.UserEntity;
import org.proj.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response =
                userService.register(request);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                userService.login(request)
        );
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LogoutResponse> logout() {

        return ResponseEntity.ok(
                userService.logout()
        );
    }

    @GetMapping("/profile")
    public ResponseEntity<RegisterResponse> getProfile(
            Authentication authentication) {

        UserEntity userDetails =
                (UserEntity) authentication.getPrincipal();

        return ResponseEntity.ok(
                userService.getUserByEmail(
                        userDetails.getUsername()
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("""
        hasRole('ADMIN')
        or
        (hasAnyRole('DOCTOR', 'RECEPTIONIST', 'PATIENT')
            and @securityService.isOwner(authentication, #id))
        """)
    public ResponseEntity<RegisterResponse> getUserById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<RegisterResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                userService.getAllUsers(
                        search,
                        page,
                        size
                )
        );
    }

    @GetMapping("/filter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RegisterResponse>> filterUsers(
            @ModelAttribute UserFilterRequest request) {

        return ResponseEntity.ok(
                userService.filterUsers(request)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("""
        hasRole('ADMIN')
        or
        (hasAnyRole('DOCTOR', 'RECEPTIONIST', 'PATIENT')
            and @securityService.isOwner(authentication, #id))
        """)
    public ResponseEntity<RegisterResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                userService.updateUser(
                        id,
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(
            @PathVariable UUID id) {

        userService.deleteUser(id);

        return ResponseEntity.ok(
                "User deleted successfully"
        );
    }

    @PostMapping("/update-last-login")
    public ResponseEntity<String> updateLastLogin(
            Authentication authentication) {

        UserEntity userDetails =
                (UserEntity) authentication.getPrincipal();

        userService.updateLastLogin(
                userDetails.getUsername()
        );

        return ResponseEntity.ok(
                "Last login time updated successfully"
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, String>> forgotPassword(
            @RequestParam String email) {

        userService.requestPasswordReset(email);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "message",
                        "If an account exists for this email, "
                                + "a password reset link has been sent."
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {

        userService.resetPassword(request);

        return ResponseEntity.ok(
                "Password reset successfully"
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<java.util.Map<String, String>> verifyEmail(
            @RequestParam String token) {

        userService.verifyEmail(token);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "message",
                        "Email verified successfully. You can now log in."
                )
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<java.util.Map<String, String>> resendVerification(
            @RequestParam String email) {

        userService.resendVerification(email);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "message",
                        "If an unverified account exists for this email, a new verification link has been sent."
                )
        );
    }
}
