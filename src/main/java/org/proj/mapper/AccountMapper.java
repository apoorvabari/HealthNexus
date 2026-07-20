package org.proj.mapper;

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
                .role(request.getRole() != null ? AccountEntity.Role.valueOf(request.getRole().trim().toUpperCase()) : null)
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isDeleted(request.getIsDeleted() != null ? request.getIsDeleted() : false)
                .build();
    }

    public AccountResponse toResponse(AccountEntity account) {
        return AccountResponse.builder()
                .id(account.getId())
                .firstName(account.getFirstName())
                .middleName(account.getMiddleName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .role(account.getRole() != null ? account.getRole().name() : null)
                .phoneNumber(account.getPhoneNumber())
                .isActive(account.getIsActive())
                .isDeleted(account.getIsDeleted())
                .message("Success")
                .build();
    }
}
