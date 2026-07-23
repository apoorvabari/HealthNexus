package org.proj.mapper;

import jakarta.validation.constraints.NotBlank;
import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.entity.AccountEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountEntity toEntity(AccountRequest request, PasswordEncoder encoder) {
        return AccountEntity.builder()
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(encoder.encode(request.getPassword()))
                .role(request.getRole() != null ? AccountEntity.Role.valueOf(request.getRole().trim().toUpperCase())
                        : null)
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isDeleted(request.getIsDeleted() != null ? request.getIsDeleted() : false)
                .build();
    }

    public void updateEntity(AccountEntity account, AccountRequest request, PasswordEncoder encoder) {
        if (request.getFirstName() != null) {
            account.setFirstName(request.getFirstName());
        }
        if (request.getMiddleName() != null) {
            account.setMiddleName(request.getMiddleName());
        }
        if (request.getLastName() != null) {
            account.setLastName(request.getLastName());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            account.setPassword(encoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            account.setRole(AccountEntity.Role.valueOf(request.getRole().trim().toUpperCase()));
        }
        if (request.getPhoneNumber() != null) {
            account.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getIsActive() != null) {
            account.setIsActive(request.getIsActive());
        }
        if (request.getIsDeleted() != null) {
            account.setIsDeleted(request.getIsDeleted());
        }
    }

    public AccountResponse toResponse(AccountEntity account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .firstName(account.getFirstName())
                .middleName(account.getMiddleName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .role(account.getRole() != null ? account.getRole().name() : null)
                .phoneNumber(account.getPhoneNumber())
                .isActive(account.getIsActive())
                .isDeleted(account.getIsDeleted())
                .lastLogin(account.getLastLogin())
                .message("Success")
                .build();
    }
}
