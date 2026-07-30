package org.proj.mapper;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.entity.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountEntity toEntity(RegisterRequest request) {
        return AccountEntity.builder()
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .role(request.getRole() != null ? AccountEntity.Role.valueOf(request.getRole().trim().toUpperCase())
                        : null)
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isDeleted(request.getIsDeleted() != null ? request.getIsDeleted() : false)
                .build();
    }

    public void updateEntity(AccountEntity account, RegisterRequest request) {
        account.setFirstName(request.getFirstName() != null ? request.getFirstName() : account.getFirstName());
        account.setMiddleName(request.getMiddleName() != null ? request.getMiddleName() : account.getMiddleName());
        account.setLastName(request.getLastName() != null ? request.getLastName() : account.getLastName());
        account.setRole(request.getRole() != null ? AccountEntity.Role.valueOf(request.getRole().trim().toUpperCase())
                : account.getRole());
        account.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : account.getPhoneNumber());
        account.setIsActive(request.getIsActive() != null ? request.getIsActive() : account.getIsActive());
        account.setIsDeleted(request.getIsDeleted() != null ? request.getIsDeleted() : account.getIsDeleted());
    }

    public RegisterResponse toResponse(AccountEntity account) {
        return RegisterResponse.builder()
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
