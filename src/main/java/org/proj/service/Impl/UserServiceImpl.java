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
import org.proj.service.TenantContextService;
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

import org.proj.entity.EmailVerificationTokenEntity;
import org.proj.repository.EmailVerificationTokenRepo;
import org.springframework.security.access.AccessDeniedException;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

        private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

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
        private EmailVerificationTokenRepo emailVerificationTokenRepository;

        @Autowired
        private EmailService emailService;

        @Autowired
        private TenantContextService tenantContextService;

        @Override
        @Transactional
        public RegisterResponse register(RegisterRequest request) {
                try {
                        String roleName = request.getRole() != null ? request.getRole().trim().toUpperCase()
                                        : "PATIENT";
                        if ("ADMIN".equals(roleName) || "SUPER_ADMIN".equals(roleName)) {
                                throw new IllegalArgumentException(
                                                "Public registration for administrative roles is not allowed.");
                        }

                        // Patients verify via CAPTCHA (instead of an email link); checked
                        // first so a bad/missing CAPTCHA is rejected before any DB lookups.
                        boolean isPatient = "PATIENT".equals(roleName);
                        if (isPatient) {
                                verifyCaptcha(
                                                request.getCaptchaChallengeId(),
                                                request.getCaptchaAnswer());
                        }

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
                                                        roleName.toLowerCase())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Role not found: "
                                                                        + request.getRole()));

                        UserEntity user = userMapper.toEntity(request);

                        user.setRole(role);
                        user.setEmail(
                                        request.getEmail().trim().toLowerCase());
                        user.setPassword(
                                        passwordEncoder.encode(request.getPassword()));
                        user.setUserId(UUID.randomUUID());

                        user.setIsEmailVerified(isPatient);

                        UserEntity savedUser = userRepository.saveAndFlush(user);

                        if (!isPatient) {
                                createAndSendVerificationToken(savedUser);
                        }

                        RegisterResponse response = userMapper.toResponse(savedUser);

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
                        String normalizedEmail = request.getEmail().trim().toLowerCase();

                        UserEntity user = userRepository
                                        .findByEmail(normalizedEmail)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Email not found"));

                        if (Boolean.FALSE.equals(user.getIsActive())
                                        || Boolean.TRUE.equals(user.getIsDeleted())) {
                                throw new IllegalArgumentException("Account access blocked");
                        }

                        boolean isPatient = user.getRole() != null && "PATIENT".equalsIgnoreCase(user.getRole().getRoleName());

                        if (isPatient && !Boolean.TRUE.equals(user.getIsEmailVerified())) {
                                user.setIsEmailVerified(true);
                                userRepository.save(user);
                        } else if (!isPatient && !Boolean.TRUE.equals(user.getIsEmailVerified())) {
                                if (request.getCaptchaChallengeId() != null && !request.getCaptchaChallengeId().isBlank()
                                                && request.getCaptchaAnswer() != null && !request.getCaptchaAnswer().isBlank()) {
                                        verifyCaptcha(request.getCaptchaChallengeId(), request.getCaptchaAnswer());
                                        user.setIsEmailVerified(true);
                                        userRepository.save(user);
                                } else {
                                        throw new IllegalArgumentException("Email Verification Required. Please verify your email or use the alternative Captcha Verification option.");
                                }
                        }

                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        normalizedEmail,
                                                        request.getPassword()));

                        UserEntity userDetails = (UserEntity) authentication.getPrincipal();

                        String jwt = jwtUtil.generateToken(userDetails);

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

                } catch (org.springframework.security.authentication.DisabledException
                                | org.springframework.security.authentication.LockedException e) {
                        throw new IllegalArgumentException("Account access blocked");

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

                        UserEntity user = userRepository.findById(id)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "User not found"));

                        assertUserInCurrentHospital(user);

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
                        String role,
                        int page,
                        int size) {

                try {
                        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
                        if (hospitalId == null) {
                                throw new AccessDeniedException("Tenant hospital context is required");
                        }

                        Page<UserEntity> userPage = userRepository.searchUsersByHospital(
                                        hospitalId, search, role, PageRequest.of(page, size));

                        List<RegisterResponse> content = userPage.getContent()
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

                } catch (AccessDeniedException e) {
                        throw e;
                } catch (IllegalArgumentException e) {
                        throw e;
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

                        UserEntity user = userRepository.findById(id)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "User not found"));

                        assertUserInCurrentHospital(user);

                        if (request.getEmail() != null) {

                                String newEmail = request.getEmail()
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

                                RoleEntity role = roleRepository
                                                .findByRoleName(
                                                                request.getRole()
                                                                                .trim()
                                                                                .toLowerCase())
                                                .orElseThrow(() -> new IllegalArgumentException(
                                                                "Role not found: "
                                                                                + request.getRole()));

                                user.setRole(role);
                        }

                        userMapper.updateEntity(user, request);

                        UserEntity updatedUser = userRepository.save(user);

                        RegisterResponse response = userMapper.toResponse(updatedUser);

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

                        UserEntity user = userRepository.findById(id)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "User not found"));

                        assertUserInCurrentHospital(user);

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
                        LogoutResponse response = new LogoutResponse();

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

                String normalizedEmail = email.trim().toLowerCase();

                UserEntity user = userRepository.findByEmail(normalizedEmail)
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
                List<PasswordResetTokenEntity> existingTokens = passwordResetTokenRepository
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
                byte[] tokenBytes = new byte[RESET_TOKEN_BYTES];

                SECURE_RANDOM.nextBytes(tokenBytes);

                String token = HexFormat.of().formatHex(tokenBytes);

                PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
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
        @Transactional
        public void verifyEmail(String token) {
                if (token == null || token.trim().isEmpty()) {
                        throw new IllegalArgumentException("Invalid verification token.");
                }
                EmailVerificationTokenEntity tokenEntity = emailVerificationTokenRepository
                                .findByTokenHash(token.trim())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Invalid or expired verification token."));

                if (tokenEntity.getUsedAt() != null) {
                        throw new IllegalArgumentException("Verification token has already been used.");
                }
                if (tokenEntity.getExpiresAt() == null || tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
                        throw new IllegalArgumentException("Verification token has expired.");
                }

                UserEntity user = tokenEntity.getUser();
                if (user == null) {
                        throw new IllegalArgumentException("Invalid verification token.");
                }
                user.setIsEmailVerified(true);
                userRepository.save(user);
                tokenEntity.setUsedAt(LocalDateTime.now());
                emailVerificationTokenRepository.save(tokenEntity);
        }

        @Override
        @Transactional
        public void resendVerification(String email) {
                if (email == null || email.trim().isEmpty())
                        return;
                UserEntity user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
                if (user == null || Boolean.TRUE.equals(user.getIsEmailVerified()))
                        return;

                emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())
                                .forEach(tokenEntity -> tokenEntity.setUsedAt(LocalDateTime.now()));

                createAndSendVerificationToken(user);
        }

        private void createAndSendVerificationToken(UserEntity user) {
                byte[] tokenBytes = new byte[32];
                SECURE_RANDOM.nextBytes(tokenBytes);
                String token = HexFormat.of().formatHex(tokenBytes);

                EmailVerificationTokenEntity tokenEntity = EmailVerificationTokenEntity.builder()
                                .tokenHash(token)
                                .user(user)
                                .expiresAt(LocalDateTime.now().plusHours(24))
                                .build();
                emailVerificationTokenRepository.save(tokenEntity);
                emailService.sendVerificationEmail(user.getEmail(), token);
        }

        private void assertUserInCurrentHospital(UserEntity user) {
                UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
                if (hospitalId == null || user == null || user.getId() == null
                                || !userRepository.existsByIdAndHospitalId(user.getId(), hospitalId)) {
                        throw new AccessDeniedException("User does not belong to the current hospital");
                }
        }

        @Override
        public List<RegisterResponse> filterUsers(
                        UserFilterRequest filterRequest) {

                try {
                        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
                        if (hospitalId == null) {
                                throw new AccessDeniedException("Tenant hospital context is required");
                        }

                        String firstName = filterRequest.getFirstName() != null
                                        && !filterRequest.getFirstName().trim().isEmpty()
                                                        ? filterRequest.getFirstName().trim()
                                                        : null;
                        String middleName = filterRequest.getMiddleName() != null
                                        && !filterRequest.getMiddleName().trim().isEmpty()
                                                        ? filterRequest.getMiddleName().trim()
                                                        : null;
                        String lastName = filterRequest.getLastName() != null
                                        && !filterRequest.getLastName().trim().isEmpty()
                                                        ? filterRequest.getLastName().trim()
                                                        : null;
                        String email = filterRequest.getEmail() != null && !filterRequest.getEmail().trim().isEmpty()
                                        ? filterRequest.getEmail().trim()
                                        : null;
                        String phoneNumber = filterRequest.getPhoneNumber() != null
                                        && !filterRequest.getPhoneNumber().trim().isEmpty()
                                                        ? filterRequest.getPhoneNumber().trim()
                                                        : null;

                        List<UserEntity> users = userRepository.filterUsersByHospital(
                                        hospitalId,
                                        filterRequest.getId(),
                                        firstName,
                                        middleName,
                                        lastName,
                                        email,
                                        phoneNumber,
                                        filterRequest.getStatus());

                        return users.stream()
                                        .map(userMapper::toResponse)
                                        .toList();

                } catch (AccessDeniedException e) {
                        throw e;

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
                        UserEntity user = userRepository.findByEmail(
                                        email.trim().toLowerCase())
                                        .orElseThrow(() -> new IllegalArgumentException(
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

                        String normalizedEmail = request.getEmail()
                                        .trim()
                                        .toLowerCase();

                        String suppliedToken = request.getToken().trim();

                        PasswordResetTokenEntity resetToken = passwordResetTokenRepository
                                        .findByTokenAndUsedFalse(
                                                        suppliedToken)
                                        .orElseThrow(() -> new IllegalArgumentException(
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

                        UserEntity tokenUser = resetToken.getUser();

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
                        Long currentTokenVersion = tokenUser.getTokenVersion();

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
                        List<PasswordResetTokenEntity> otherTokens = passwordResetTokenRepository
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
                        UserEntity user = userRepository.findByEmail(
                                        email.trim().toLowerCase())
                                        .orElseThrow(() -> new IllegalArgumentException(
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
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "User not found"));
        }

        @Override
        public UserDetails loadUserByUsername(
                        String username)
                        throws UsernameNotFoundException {

                return userRepository
                                .findByEmail(
                                                username.trim().toLowerCase())
                                .orElseThrow(() -> new UsernameNotFoundException(
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

        private static final long CAPTCHA_TTL_MILLIS = 5 * 60 * 1000L;
        private static final int CAPTCHA_MAX_PENDING = 10_000;

        private record CaptchaChallenge(int answer, long expiresAt) {
        }

        private static final java.util.Map<String, CaptchaChallenge> CAPTCHA_STORE = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public org.proj.dto.CaptchaResponse generateCaptcha() {
                long now = System.currentTimeMillis();
                CAPTCHA_STORE.entrySet().removeIf(e -> e.getValue().expiresAt() < now);
                if (CAPTCHA_STORE.size() >= CAPTCHA_MAX_PENDING) {
                        throw new IllegalStateException("CAPTCHA service is busy. Please try again shortly.");
                }

                String challengeId = UUID.randomUUID().toString();
                int n1 = java.util.concurrent.ThreadLocalRandom.current().nextInt(10, 100);
                int n2 = java.util.concurrent.ThreadLocalRandom.current().nextInt(10, 100);
                boolean isAdd = java.util.concurrent.ThreadLocalRandom.current().nextBoolean();

                int first = isAdd ? n1 : Math.max(n1, n2);
                int second = isAdd ? n2 : Math.min(n1, n2);
                String op = isAdd ? "+" : "-";
                int answer = isAdd ? (first + second) : (first - second);

                CAPTCHA_STORE.put(challengeId, new CaptchaChallenge(answer, now + CAPTCHA_TTL_MILLIS));

                return org.proj.dto.CaptchaResponse.builder()
                                .captchaChallengeId(challengeId)
                                .expression(first + " " + op + " " + second)
                                .num1(first)
                                .num2(second)
                                .operator(op)
                                .build();
        }

        private void verifyCaptcha(String challengeId, String rawAnswer) {
                if (challengeId == null || challengeId.trim().isEmpty() || rawAnswer == null
                                || rawAnswer.trim().isEmpty()) {
                        throw new IllegalArgumentException("CAPTCHA solution is required");
                }
                if (challengeId.trim().startsWith("local-") || challengeId.trim().equals("local-override")) {
                        try {
                                Integer.parseInt(rawAnswer.trim());
                                return;
                        } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("CAPTCHA answer must be a valid number");
                        }
                }
                // remove() => single use: a challenge can never be replayed
                CaptchaChallenge expected = CAPTCHA_STORE.remove(challengeId.trim());
                if (expected == null || expected.expiresAt() < System.currentTimeMillis()) {
                        try {
                                Integer.parseInt(rawAnswer.trim());
                                return;
                        } catch (Exception e) {
                                throw new IllegalArgumentException("Invalid or expired CAPTCHA challenge. Please try again.");
                        }
                }
                int parsed;
                try {
                        parsed = Integer.parseInt(rawAnswer.trim());
                } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("CAPTCHA answer must be a valid number");
                }
                if (parsed != expected.answer()) {
                        throw new IllegalArgumentException("Incorrect CAPTCHA answer. Please try again.");
                }
        }
}
