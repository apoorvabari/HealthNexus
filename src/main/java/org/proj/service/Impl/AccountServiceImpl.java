package org.proj.service.Impl;

import org.proj.service.AccountService;
import java.util.List;
import java.util.UUID;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.LogoutResponse;
import org.proj.dto.PasswordResetRequest;
import org.proj.entity.AccountEntity;
import org.proj.mapper.AccountMapper;
import org.proj.repository.AccountRepo;
import org.proj.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepo accountRepository;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        AccountEntity savedAccount = null;
        try {
            // 1. Check MySQL First
            if (accountRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new IllegalArgumentException("User already exists with email: " + request.getEmail());
            }
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                    && accountRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new IllegalArgumentException(
                        "User already exists with phone number: " + request.getPhoneNumber());
            }

            AccountEntity account = accountMapper.toEntity(request);

            // Encrypt the password using BCrypt
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                account.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            savedAccount = accountRepository.save(account);

            RegisterResponse response = accountMapper.toResponse(savedAccount);
            response.setMessage("Account registered successfully");

            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to register account. Please try again. " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required for login.");
        }
        System.out.println("DEBUG: Attempting login for email: '" + request.getEmail().trim() + "'");

        AccountEntity account = accountRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                        "Account not found in database for email: " + request.getEmail().trim()));

        if (Boolean.FALSE.equals(account.getIsActive()) || Boolean.TRUE.equals(account.getIsDeleted())) {
            throw new IllegalArgumentException("Account is inactive or disabled.");
        }

        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid password");
        }

        String accessToken = jwtService.generateToken(
                account.getEmail(),
                account.getRole() != null ? account.getRole().name() : "PATIENT"
        );

        return LoginResponse.builder()
                .id(account.getUserId())
                .userId(account.getUserId())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .role(account.getRole() != null ? account.getRole().name() : null)
                .accessToken(accessToken)
                .refreshToken(null)
                .message("Login successful")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RegisterResponse getAccountById(UUID id, String currentUserEmail) {
        try {
            validateId(id);
            AccountEntity account = accountRepository.findByUserId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            AccountEntity currentUser = accountRepository.findByEmailIgnoreCase(currentUserEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

            if (currentUser.getRole() == AccountEntity.Role.PATIENT && !account.getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new org.springframework.security.access.AccessDeniedException("Access Denied: You do not have permission to view this account.");
            }

            return accountMapper.toResponse(account);
        } catch (IllegalArgumentException | org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch account.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegisterResponse> getAllAccounts() {
        try {
            List<AccountEntity> accounts = accountRepository.findByIsActiveTrue();

            return accounts.stream()
                    .map(accountMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch account list.");
        }
    }

    @Override
    @Transactional
    public RegisterResponse updateAccount(UUID id, RegisterRequest request, String currentUserEmail) {
        try {
            validateId(id);
            AccountEntity account = accountRepository.findByUserId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            AccountEntity currentUser = accountRepository.findByEmailIgnoreCase(currentUserEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

            if (currentUser.getRole() == AccountEntity.Role.PATIENT && !account.getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new org.springframework.security.access.AccessDeniedException("Access Denied: You do not have permission to modify this account.");
            }

            String oldEmail = account.getEmail();

            if (request.getEmail() != null) {
                String newEmail = request.getEmail().trim();
                if (!account.getEmail().equalsIgnoreCase(newEmail)
                        && accountRepository.existsByEmailIgnoreCase(newEmail)) {
                    throw new IllegalArgumentException("Email already exists");
                }
                account.setEmail(newEmail);
            }

            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                account.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            accountMapper.updateEntity(account, request);

            AccountEntity updatedAccount = accountRepository.save(account);

            RegisterResponse response = accountMapper.toResponse(updatedAccount);
            response.setMessage("Account updated successfully");

            return response;
        } catch (IllegalArgumentException | org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update account.");
        }
    }

    @Override
    @Transactional
    public void deleteAccount(UUID id) {
        try {
            validateId(id);
            AccountEntity account = accountRepository.findByUserId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            if (Boolean.TRUE.equals(account.getIsDeleted())) {
                throw new IllegalArgumentException("Account is already deleted.");
            }

            account.setIsDeleted(true);
            account.setIsActive(false);

            accountRepository.save(account);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete account.");
        }
    }

    @Override
    public LogoutResponse logout(String userId) {
        try {
            LogoutResponse response = new LogoutResponse();
            if (userId != null && !userId.isBlank()) {
                response.setMessage("Logged out successfully for user " + userId);
            } else {
                response.setMessage("Logged out successfully");
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Unable to logout.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegisterResponse> filterAccounts(AccountFilterRequest filterRequest) {
        try {
            Boolean isActive = null;
            Boolean isDeleted = null;
            String status = filterRequest.getStatus();

            if (status != null && !status.trim().isEmpty()) {
                if ("ACTIVE".equalsIgnoreCase(status.trim())) {
                    isActive = true;
                    isDeleted = false;
                } else if ("DELETED".equalsIgnoreCase(status.trim())) {
                    isActive = false;
                    isDeleted = true;
                }
            }

            String firstName = filterRequest.getFirstName();
            if (firstName != null && firstName.trim().isEmpty()) {
                firstName = null;
            } else if (firstName != null) {
                firstName = firstName.trim();
            }

            String middleName = filterRequest.getMiddleName();
            if (middleName != null && middleName.trim().isEmpty()) {
                middleName = null;
            } else if (middleName != null) {
                middleName = middleName.trim();
            }

            String lastName = filterRequest.getLastName();
            if (lastName != null && lastName.trim().isEmpty()) {
                lastName = null;
            } else if (lastName != null) {
                lastName = lastName.trim();
            }

            String email = filterRequest.getEmail();
            if (email != null && email.trim().isEmpty()) {
                email = null;
            } else if (email != null) {
                email = email.trim();
            }

            String phoneNumber = filterRequest.getPhoneNumber();
            if (phoneNumber != null && phoneNumber.trim().isEmpty()) {
                phoneNumber = null;
            } else if (phoneNumber != null) {
                phoneNumber = phoneNumber.trim();
            }

            List<AccountEntity> accounts = accountRepository.filterAccounts(
                    filterRequest.getId(),
                    firstName,
                    middleName,
                    lastName,
                    email,
                    phoneNumber,
                    isActive,
                    isDeleted);

            return accounts.stream()
                    .map(accountMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to filter accounts.", e);
        }
    }

    private void validateId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Account Id is required.");
        }
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        AccountEntity account = accountRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // update local password
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisterResponse getAccountByEmail(String email) {
        AccountEntity account = accountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return accountMapper.toResponse(account);
    }

}