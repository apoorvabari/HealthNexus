package org.proj.service.Impl;

import java.util.List;

import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.entity.AccountEntity;
import org.proj.mapper.AccountMapper;
import org.proj.repository.AccountRepo;
import org.proj.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
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
            if (accountRepository.existsByEmail(request.getEmail())) {
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
                if (!account.getEmail().equalsIgnoreCase(newEmail) && accountRepository.existsByEmail(newEmail)) {
                    throw new IllegalArgumentException("Email already exists");
                }
                account.setEmail(newEmail);
            }

            account.setFirstName(request.getFirstName());
            account.setMiddleName(request.getMiddleName());
            account.setLastName(request.getLastName());
            account.setPassword(passwordEncoder.encode(request.getPassword()));
            account.setRole(request.getRole() != null ? AccountEntity.Role.valueOf(request.getRole().trim().toUpperCase()) : null);
            account.setPhoneNumber(request.getPhoneNumber());

            if (request.getIsActive() != null) {
                account.setIsActive(request.getIsActive());
            }

            if (request.getIsDeleted() != null) {
                account.setIsDeleted(request.getIsDeleted());
            }

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
            AccountEntity account = accountRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

            if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
                throw new IllegalArgumentException("Invalid email or password");
            }

            LoginResponse response = new LoginResponse();
            response.setMessage("Login Successful");
            response.setRole(account.getRole() != null ? account.getRole().name() : null);

            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to login.");
        }
    }

    @Override
    public List<AccountResponse> filterAccounts(Long id, String name, String email, String phoneNumber, String status) {
        try {
            Boolean isActive = null;
            Boolean isDeleted = null;

            if (status != null) {
                if ("ACTIVE".equalsIgnoreCase(status)) {
                    isActive = true;
                    isDeleted = false;
                } else if ("DELETED".equalsIgnoreCase(status)) {
                    isActive = false;
                    isDeleted = true;
                }
            }

            List<AccountEntity> accounts = accountRepository.filterAccounts(id, name, email, phoneNumber, isActive, isDeleted);

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
}