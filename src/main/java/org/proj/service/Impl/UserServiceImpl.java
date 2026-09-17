package org.proj.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
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
import org.proj.entity.PasswordResetTokenEntity;
import org.proj.entity.RoleEntity;
import org.proj.entity.UserEntity;
import org.proj.mapper.UserMapper;
import org.proj.repository.PasswordResetTokenRepo;
import org.proj.repository.RoleRepo;
import org.proj.repository.UserRepo;
import org.proj.security.JwtUtil;
import org.proj.service.EmailService;
import org.proj.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final Logger logger =
            LoggerFactory.getLogger(UserServiceImpl.class);

    private static final int RESET_TOKEN_BYTES = 32;
    private static final int RESET_TOKEN_EXPIRATION_MINUTES = 30;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private RoleRepo roleRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenRepo passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        try {
            if (userRepository.existsByEmail(
                    request.getEmail().trim().toLowerCase())) {

                throw new IllegalArgumentException(
                        "User email already exists");
            }

            if (userRepository.existsByPhoneNumber(
                    request.getPhoneNumber())) {

                throw new IllegalArgumentException(
                        "Phone number already exists");
            }

            RoleEntity role = roleRepository
                    .findByRoleName(
                            request.getRole().trim().toLowerCase())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Role not found: "
                                            + request.getRole()));

            UserEntity user = userMapper.toEntity(request);

            user.setRole(role);
            user.setEmail(
                    request.getEmail().trim().toLowerCase());
            user.setPassword(
                    passwordEncoder.encode(request.getPassword()));
            user.setUserId(UUID.randomUUID());

            UserEntity savedUser =
                    userRepository.saveAndFlush(user);

            RegisterResponse response =
                    userMapper.toResponse(savedUser);

            response.setMessage(
                    "User registered successfully");

            return response;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to register user. Please try again.",
                    e);
        }
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            UserEntity user = userRepository
                    .findByEmail(
                            request.getEmail()
                                    .trim()
                                    .toLowerCase())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Email not found"));

            if (Boolean.FALSE.equals(user.getIsActive())
                    || Boolean.TRUE.equals(user.getIsDeleted())) {

                throw new IllegalArgumentException(
                        "Account is inactive or disabled.");
            }

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.getEmail(),
                                    request.getPassword()));

            UserEntity userDetails =
                    (UserEntity) authentication.getPrincipal();

            String jwt =
                    jwtUtil.generateToken(userDetails);

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            return LoginResponse.builder()
                    .id(user.getId())
                    .userId(user.getUserId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .role(user.getRole() != null
                            ? user.getRole().getRoleName()
                            : null)
                    .accessToken(jwt)
                    .refreshToken(null)
                    .lastLogin(user.getLastLogin())
                    .message("Login successful")
                    .build();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new IllegalArgumentException(
                    "Invalid email or password.");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to login. Please try again.",
                    e);
        }
    }

    @Override
    public RegisterResponse getUserById(UUID id) {
        try {
            validateId(id);

            UserEntity user =
                    userRepository.findById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "User not found"));

            return userMapper.toResponse(user);

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch user.");
        }
    }

    @Override
    public PageResponse<RegisterResponse> getAllUsers(
            String search,
            int page,
            int size) {

        try {
            Page<UserEntity> userPage =
                    userRepository.searchUsers(
                            search,
                            PageRequest.of(page, size));

            List<RegisterResponse> content =
                    userPage.getContent()
                            .stream()
                            .map(userMapper::toResponse)
                            .toList();

            return new PageResponse<>(
                    content,
                    userPage.getNumber(),
                    userPage.getSize(),
                    userPage.getTotalElements(),
                    userPage.getTotalPages(),
                    userPage.isLast());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch user list.",
                    e);
        }
    }

    @Override
    public RegisterResponse updateUser(
            UUID id,
            RegisterRequest request) {

        try {
            validateId(id);

            UserEntity user =
                    userRepository.findById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "User not found"));

            if (request.getEmail() != null) {

                String newEmail =
                        request.getEmail()
                                .trim()
                                .toLowerCase();

                if (!user.getEmail().equals(newEmail)
                        && userRepository.existsByEmail(newEmail)) {

                    throw new IllegalArgumentException(
                            "Email already exists");
                }

                user.setEmail(newEmail);
            }

            if (request.getRole() != null) {

                RoleEntity role =
                        roleRepository
                                .findByRoleName(
                                        request.getRole()
                                                .trim()
                                                .toLowerCase())
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Role not found: "
                                                        + request.getRole()));

                user.setRole(role);
            }

            userMapper.updateEntity(user, request);

            UserEntity updatedUser =
                    userRepository.save(user);

            RegisterResponse response =
                    userMapper.toResponse(updatedUser);

            response.setMessage(
                    "User updated successfully");

            return response;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to update user.",
                    e);
        }
    }

    @Override
    public void deleteUser(UUID id) {
        try {
            validateId(id);

            UserEntity user =
                    userRepository.findById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "User not found"));

            if (Boolean.TRUE.equals(user.getIsDeleted())) {
                throw new IllegalArgumentException(
                        "User is already deleted.");
            }

            user.setIsDeleted(true);
            user.setIsActive(false);

            userRepository.save(user);

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to delete user.");
        }
    }

    @Override
    public LogoutResponse logout() {
        try {
            LogoutResponse response =
                    new LogoutResponse();

            response.setMessage(
                    "Logged out successfully");

            return response;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to logout.");
        }
    }

    /**
     * Creates a secure, single-use password reset token.
     *
     * The response from the controller remains generic so that
     * the API does not reveal whether an email exists.
     */
    @Override
    @Transactional
    public void requestPasswordReset(String email) {

        if (email == null || email.trim().isEmpty()) {
            return;
        }

        String normalizedEmail =
                email.trim().toLowerCase();

        UserEntity user =
                userRepository.findByEmail(normalizedEmail)
                        .orElse(null);

        /*
         * Do not reveal whether the email exists.
         */
        if (user == null) {
            return;
        }

        /*
         * Do not issue password-reset links to disabled/deleted
         * accounts.
         */
        if (Boolean.FALSE.equals(user.getIsActive())
                || Boolean.TRUE.equals(user.getIsDeleted())) {
            return;
        }

        /*
         * Invalidate all previous unused reset tokens.
         */
        List<PasswordResetTokenEntity> existingTokens =
                passwordResetTokenRepository
                        .findAllByUserAndUsedFalse(user);

        if (!existingTokens.isEmpty()) {
            existingTokens.forEach(token -> token.setUsed(true));
            passwordResetTokenRepository.saveAll(existingTokens);
        }

        /*
         * Generate 256 bits of cryptographically secure randomness.
         *
         * 32 bytes = 64 hexadecimal characters, matching the
         * PasswordResetTokenEntity column length.
         */
        byte[] tokenBytes =
                new byte[RESET_TOKEN_BYTES];

        SECURE_RANDOM.nextBytes(tokenBytes);

        String token =
                HexFormat.of().formatHex(tokenBytes);

        PasswordResetTokenEntity resetToken =
                PasswordResetTokenEntity.builder()
                        .token(token)
                        .user(user)
                        .expiryDate(
                                LocalDateTime.now()
                                        .plusMinutes(
                                                RESET_TOKEN_EXPIRATION_MINUTES))
                        .used(false)
                        .build();

        passwordResetTokenRepository.save(resetToken);

        /*
         * Send only the generated random token.
         * The email service constructs the frontend reset link.
         */
        emailService.sendPasswordResetEmail(
                normalizedEmail,
                token);
    }

    @Override
    public void verifyEmail(String token) {
        throw new UnsupportedOperationException(
                "Not implemented yet");
    }

    @Override
    public void resendVerification(String email) {
        throw new UnsupportedOperationException(
                "Not implemented yet");
    }

    @Override
    public List<RegisterResponse> filterUsers(
            UserFilterRequest filterRequest) {

        try {
            Boolean isActive = null;
            Boolean isDeleted = null;

            String status =
                    filterRequest.getStatus();

            if (status != null) {

                if ("ACTIVE".equalsIgnoreCase(status)) {
                    isActive = true;
                    isDeleted = false;

                } else if ("DELETED".equalsIgnoreCase(status)) {
                    isActive = false;
                    isDeleted = true;
                }
            }

            String firstName =
                    filterRequest.getFirstName();

            if (firstName != null
                    && firstName.trim().isEmpty()) {

                firstName = null;

            } else if (firstName != null) {

                firstName = firstName.trim();
            }

            String middleName =
                    filterRequest.getMiddleName();

            if (middleName != null
                    && middleName.trim().isEmpty()) {

                middleName = null;

            } else if (middleName != null) {

                middleName = middleName.trim();
            }

            String lastName =
                    filterRequest.getLastName();

            if (lastName != null
                    && lastName.trim().isEmpty()) {

                lastName = null;

            } else if (lastName != null) {

                lastName = lastName.trim();
            }

            String email =
                    filterRequest.getEmail();

            if (email != null
                    && email.trim().isEmpty()) {

                email = null;

            } else if (email != null) {

                email = email.trim();
            }

            String phoneNumber =
                    filterRequest.getPhoneNumber();

            if (phoneNumber != null
                    && phoneNumber.trim().isEmpty()) {

                phoneNumber = null;

            } else if (phoneNumber != null) {

                phoneNumber = phoneNumber.trim();
            }

            List<UserEntity> users =
                    userRepository.filterUsers(
                            filterRequest.getId(),
                            firstName,
                            middleName,
                            lastName,
                            email,
                            phoneNumber,
                            isActive,
                            isDeleted);

            return users.stream()
                    .map(userMapper::toResponse)
                    .toList();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            logger.error(
                    "Error filtering users",
                    e);

            throw new RuntimeException(
                    "Unable to filter users.",
                    e);
        }
    }

    private void validateId(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "User Id is required.");
        }
    }

    @Override
    public void updateLastLogin(String email) {

        try {
            UserEntity user =
                    userRepository.findByEmail(
                            email.trim().toLowerCase())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "User not found"));

            user.setLastLogin(
                    LocalDateTime.now());

            userRepository.save(user);

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to update last login.",
                    e);
        }
    }

    /**
     * Secure password reset.
     *
     * The email and token must refer to the same user.
     * The token must be unused and unexpired.
     * After successful reset the token is permanently invalidated
     * and tokenVersion is incremented to invalidate existing JWTs.
     */
    @Override
    @Transactional
    public void resetPassword(
            PasswordResetRequest request) {

        try {
            if (request == null) {
                throw new IllegalArgumentException(
                        "Invalid password reset request.");
            }

            if (request.getEmail() == null
                    || request.getEmail().trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Invalid password reset request.");
            }

            if (request.getToken() == null
                    || request.getToken().trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Invalid or expired password reset token.");
            }

            if (request.getNewPassword() == null
                    || request.getNewPassword().trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "New password is required.");
            }

            if (request.getConfirmPassword() == null
                    || !request.getNewPassword()
                            .equals(request.getConfirmPassword())) {

                throw new IllegalArgumentException(
                        "Passwords do not match");
            }

            String normalizedEmail =
                    request.getEmail()
                            .trim()
                            .toLowerCase();

            String suppliedToken =
                    request.getToken().trim();

            PasswordResetTokenEntity resetToken =
                    passwordResetTokenRepository
                            .findByTokenAndUsedFalse(
                                    suppliedToken)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Invalid or expired password reset token."));

            /*
             * Token expiry check.
             */
            if (resetToken.getExpiryDate() == null
                    || resetToken.getExpiryDate()
                            .isBefore(LocalDateTime.now())) {

                resetToken.setUsed(true);
                passwordResetTokenRepository.save(resetToken);

                throw new IllegalArgumentException(
                        "Invalid or expired password reset token.");
            }

            UserEntity tokenUser =
                    resetToken.getUser();

            if (tokenUser == null
                    || tokenUser.getEmail() == null) {

                resetToken.setUsed(true);
                passwordResetTokenRepository.save(resetToken);

                throw new IllegalArgumentException(
                        "Invalid or expired password reset token.");
            }

            /*
             * Prevent token/email mismatch attacks.
             */
            if (!tokenUser.getEmail()
                    .trim()
                    .equalsIgnoreCase(normalizedEmail)) {

                throw new IllegalArgumentException(
                        "Invalid or expired password reset token.");
            }

            /*
             * Do not allow password reset for an inactive/deleted
             * account.
             */
            if (Boolean.FALSE.equals(
                        tokenUser.getIsActive())
                    || Boolean.TRUE.equals(
                        tokenUser.getIsDeleted())) {

                throw new IllegalArgumentException(
                        "Password reset is not available for this account.");
            }

            /*
             * Change password only after every token validation
             * above succeeds.
             */
            tokenUser.setPassword(
                    passwordEncoder.encode(
                            request.getNewPassword()));

            /*
             * Invalidate every existing JWT issued before this
             * password reset.
             */
            Long currentTokenVersion =
                    tokenUser.getTokenVersion();

            if (currentTokenVersion == null) {
                currentTokenVersion = 0L;
            }

            tokenUser.setTokenVersion(
                    currentTokenVersion + 1L);

            userRepository.save(tokenUser);

            /*
             * Make the reset token permanently single-use.
             */
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);

            /*
             * Invalidate any other outstanding reset tokens
             * belonging to the same account.
             */
            List<PasswordResetTokenEntity> otherTokens =
                    passwordResetTokenRepository
                            .findAllByUserAndUsedFalse(
                                    tokenUser);

            if (!otherTokens.isEmpty()) {
                otherTokens.forEach(token -> token.setUsed(true));
                passwordResetTokenRepository.saveAll(otherTokens);
            }

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            logger.error(
                    "Unable to reset password",
                    e);

            throw new RuntimeException(
                    "Unable to reset password.",
                    e);
        }
    }

    @Override
    public RegisterResponse getUserByEmail(
            String email) {

        try {
            UserEntity user =
                    userRepository.findByEmail(
                            email.trim().toLowerCase())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "User not found"));

            return userMapper.toResponse(user);

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch user by email.",
                    e);
        }
    }

    @Override
    public UserEntity findUserById(UUID accountId) {

        return userRepository.findById(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"));
    }

    @Override
    public UserDetails loadUserByUsername(
            String username)
            throws UsernameNotFoundException {

        return userRepository
                .findByEmail(
                        username.trim().toLowerCase())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: "
                                        + username));
    }

    @Override
    public void save(UserEntity user) {
        userRepository.save(user);
    }

    @Override
    public long count() {
        return userRepository.count();
    }

    @Override
    public long countByIsActiveTrue() {
        return userRepository.countByIsActiveTrue();
    }

    @Override
    public long countByIsDeletedFalse() {
        return userRepository.countByIsDeletedFalse();
    }

    @Override
    public long countByIsActiveTrueAndIsDeletedFalse() {
        return userRepository
                .countByIsActiveTrueAndIsDeletedFalse();
    }
}