package org.proj.service.Impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.LogoutResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.entity.AccountEntity;
import org.proj.mapper.AccountMapper;
import org.proj.repository.AccountRepo;
import org.proj.service.AccountService;
import org.proj.service.KeycloakAdminService;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepo accountRepository;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private KeycloakAdminService keycloakAdminService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (accountRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (accountRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        AccountEntity account = accountMapper.toEntity(request);
        AccountEntity savedAccount = null;
        try {
            savedAccount = accountRepository.saveAndFlush(account);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save account in database: " + e.getMessage(), e);
        }

        String keycloakUserId = null;
        try {
            keycloakUserId = keycloakAdminService.createUserInKeycloak(request);
            if (keycloakUserId == null || keycloakUserId.isBlank()) {
                throw new RuntimeException("Keycloak returned an empty User ID");
            }

            savedAccount.setUserId(java.util.UUID.fromString(keycloakUserId));
            savedAccount = accountRepository.saveAndFlush(savedAccount);

            RegisterResponse response = accountMapper.toResponse(savedAccount);
            response.setMessage("Account registered successfully");
            return response;
        } catch (Exception e) {
            if (savedAccount != null) {
                try {
                    accountRepository.delete(savedAccount);
                    accountRepository.flush();
                } catch (Exception rollbackException) {
                    System.err.println("CRITICAL: Failed to rollback MySQL database record during registration failure: " + rollbackException.getMessage());
                }
            }
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new RuntimeException("Unable to register account. Please try again. " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. Verify if the account exists in the local database first
        AccountEntity account = accountRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        if (Boolean.FALSE.equals(account.getIsActive()) || Boolean.TRUE.equals(account.getIsDeleted())) {
            throw new IllegalArgumentException("Account is inactive or disabled.");
        }

        // 2. Authenticate with Keycloak to retrieve JWT token details
        AccessTokenResponse tokenResponse = keycloakAdminService.authenticateAndGetToken(request.getEmail(), request.getPassword());

        account.setLastLogin(LocalDateTime.now());
        accountRepository.save(account);

        return LoginResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .role(account.getRole() != null ? account.getRole().name() : null)
                .accessToken(tokenResponse.getToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .message("Login successful")
                .build();
    }

    @Override
    public RegisterResponse getAccountById(UUID id) {
        try {
            validateId(id);
            AccountEntity account = accountRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            return accountMapper.toResponse(account);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch account.");
        }
    }

    @Override
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
    public RegisterResponse updateAccount(UUID id, RegisterRequest request) {
        try {
            validateId(id);
            AccountEntity account = accountRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            String oldEmail = account.getEmail();

            if (request.getEmail() != null) {
                String newEmail = request.getEmail().trim();
                if (!account.getEmail().equalsIgnoreCase(newEmail) && accountRepository.existsByEmailIgnoreCase(newEmail)) {
                    throw new IllegalArgumentException("Email already exists");
                }
                account.setEmail(newEmail);
            }

            accountMapper.updateEntity(account, request);

            AccountEntity updatedAccount = accountRepository.save(account);

            // Sync updates to Keycloak
            keycloakAdminService.updateUserInKeycloak(
                    oldEmail,
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName()
            );

            RegisterResponse response = accountMapper.toResponse(updatedAccount);
            response.setMessage("Account updated successfully");

            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update account.");
        }
    }

    @Override
    public void deleteAccount(UUID id) {
        try {
            validateId(id);
            AccountEntity account = accountRepository.findById(id)
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
    public List<RegisterResponse> filterAccounts(AccountFilterRequest filterRequest) {
        try {
            Boolean isActive = null;
            Boolean isDeleted = null;
            String status = filterRequest.getStatus();

            if (status != null) {
                if ("ACTIVE".equalsIgnoreCase(status)) {
                    isActive = true;
                    isDeleted = false;
                } else if ("DELETED".equalsIgnoreCase(status)) {
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
    public void updateLastLogin(String email, String keycloakUserId) {
        AccountEntity account = accountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            throw new IllegalArgumentException("Keycloak User ID is required.");
        }

        try {
            account.setUserId(java.util.UUID.fromString(keycloakUserId));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Keycloak User ID format.");
        }

        account.setLastLogin(LocalDateTime.now());
        accountRepository.save(account);
    }

    @Override
    public void resetPassword(org.proj.dto.PasswordResetRequest request) {
        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        accountRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        keycloakAdminService.resetUserPassword(request.getEmail(), request.getNewPassword());
    }

    @Override
    public RegisterResponse getAccountByEmail(String email) {
        AccountEntity account = accountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return accountMapper.toResponse(account);
    }

}