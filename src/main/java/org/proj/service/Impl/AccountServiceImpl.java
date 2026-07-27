package org.proj.service.Impl;

import java.time.LocalDateTime;
import java.util.List;

import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.LogoutResponse;
import org.proj.entity.AccountEntity;
import org.proj.mapper.AccountMapper;
import org.proj.repository.AccountRepo;
import org.proj.service.AccountService;
import org.proj.service.KeycloakAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepo accountRepository;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private KeycloakAdminService keycloakAdminService;

    @Override
    public AccountResponse register(AccountRequest request) {
        String keycloakUserId = null;
        try {
            if (accountRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }

            keycloakUserId = keycloakAdminService.createUserInKeycloak(request);

            AccountEntity account = accountMapper.toEntity(request);
            account.setUserId(java.util.UUID.fromString(keycloakUserId));

            AccountEntity savedAccount = accountRepository.save(account);

            AccountResponse response = accountMapper.toResponse(savedAccount);
            response.setMessage("Account registered successfully");

            return response;
        } catch (IllegalArgumentException e) {
            if (keycloakUserId != null) {
                keycloakAdminService.deleteUserInKeycloak(keycloakUserId);
            }
            throw e;
        } catch (Exception e) {
            if (keycloakUserId != null) {
                keycloakAdminService.deleteUserInKeycloak(keycloakUserId);
            }
            throw new RuntimeException("Unable to register account. Please try again. " + e.getMessage(), e);
        }
    }

    @Override
    public AccountResponse getAccountById(Long id) {
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
    public List<AccountResponse> getAllAccounts() {
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
    public AccountResponse updateAccount(Long id, AccountRequest request) {
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

            AccountResponse response = accountMapper.toResponse(updatedAccount);
            response.setMessage("Account updated successfully");

            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update account.");
        }
    }

    @Override
    public void deleteAccount(Long id) {
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
    public List<AccountResponse> filterAccounts(AccountFilterRequest filterRequest) {
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

            List<AccountEntity> accounts = accountRepository.filterAccounts(
                    filterRequest.getId(),
                    filterRequest.getName(),
                    filterRequest.getEmail(),
                    filterRequest.getPhoneNumber(),
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

    private void validateId(Long id) {
        String errorMsg = (id == null) ? "Account Id is required." : (id <= 0) ? "Invalid Account Id." : null;
        if (errorMsg != null) {
            throw new IllegalArgumentException(errorMsg);
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
    public AccountResponse getAccountByEmail(String email) {
        AccountEntity account = accountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return accountMapper.toResponse(account);
    }

}