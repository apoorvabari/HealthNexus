package org.proj.service.Impl;

import java.time.LocalDateTime;
import java.util.List;

import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.dto.LogoutResponse;
import org.proj.entity.AccountEntity;
import org.proj.mapper.AccountMapper;
import org.proj.repository.AccountRepo;
import org.proj.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepo accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountMapper accountMapper;

    @Override
    public AccountResponse register(AccountRequest request) {
        try {
            if (accountRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }

            AccountEntity account = accountMapper.toEntity(request, passwordEncoder);
            AccountEntity savedAccount = accountRepository.save(account);

            AccountResponse response = accountMapper.toResponse(savedAccount);
            response.setMessage("Account registered successfully");

            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to register account. Please try again.");
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

            if (request.getEmail() != null) {
                String newEmail = request.getEmail().trim();
                if (!account.getEmail().equalsIgnoreCase(newEmail) && accountRepository.existsByEmailIgnoreCase(newEmail)) {
                    throw new IllegalArgumentException("Email already exists");
                }
                account.setEmail(newEmail);
            }

            accountMapper.updateEntity(account, request, passwordEncoder);

            AccountEntity updatedAccount = accountRepository.save(account);

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
    public LoginResponse login(LoginRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new IllegalArgumentException("Email is required");
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password is required");
            }

            String trimmedEmail = request.getEmail().trim();
            AccountEntity account = accountRepository.findByEmailIgnoreCase(trimmedEmail)
                    .orElseThrow(() -> new BadCredentialsException("Invalid email "));

            if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
                throw new BadCredentialsException("Invalid password");
            }

            if (Boolean.TRUE.equals(account.getIsDeleted()) || Boolean.FALSE.equals(account.getIsActive())) {
                throw new IllegalArgumentException("Account is inactive or disabled. Please contact support.");
            }

            LoginResponse response = LoginResponse.builder()
                    .id(account.getId())
                    .userId(account.getUserId())
                    .firstName(account.getFirstName())
                    .lastName(account.getLastName())
                    .email(account.getEmail())
                    .role(account.getRole() != null ? account.getRole().name() : null)
                    .message("Login Successful")
                    .build();

            account.setLastLogin(LocalDateTime.now());
            accountRepository.save(account);

            return response;
        } catch (IllegalArgumentException | BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to login. Please check credentials or try again.");
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

    @Override
    public void resetPassword(LoginRequest.PasswordResetRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        AccountEntity account = accountRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new IllegalArgumentException("Account not found with this email"));

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
    }

    private void validateId(Long id) {
        String errorMsg = (id == null) ? "Account Id is required." : (id <= 0) ? "Invalid Account Id." : null;
        if (errorMsg != null) {
            throw new IllegalArgumentException(errorMsg);
        }
    }
}