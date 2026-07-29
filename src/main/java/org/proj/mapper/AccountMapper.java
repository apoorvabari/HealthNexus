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
                .role(parseRole(request.getRole()))
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isDeleted(request.getIsDeleted() != null ? request.getIsDeleted() : false)
                .build();
    }

    private AccountEntity.Role parseRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return AccountEntity.Role.PATIENT;
        }
        try {
            return AccountEntity.Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid role: '" + roleStr + "'. Allowed roles are PATIENT, DOCTOR, RECEPTIONIST, ADMIN.");
        }
    }

    public void updateEntity(AccountEntity account, RegisterRequest request) {
        account.setFirstName(request.getFirstName() != null ? request.getFirstName() : account.getFirstName());
        account.setMiddleName(request.getMiddleName() != null ? request.getMiddleName() : account.getMiddleName());
        account.setLastName(request.getLastName() != null ? request.getLastName() : account.getLastName());
        account.setRole(request.getRole() != null && !request.getRole().isBlank() ? parseRole(request.getRole())
                : account.getRole());
        account.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : account.getPhoneNumber());
        account.setIsActive(request.getIsActive() != null ? request.getIsActive() : account.getIsActive());
        account.setIsDeleted(request.getIsDeleted() != null ? request.getIsDeleted() : account.getIsDeleted());
    }

    public RegisterResponse toResponse(AccountEntity account) {
        return RegisterResponse.builder()
                .id(account.getUserId())
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
