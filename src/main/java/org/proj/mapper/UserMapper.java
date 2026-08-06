package org.proj.mapper;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(RegisterRequest request) {
        return UserEntity.builder()
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isDeleted(request.getIsDeleted() != null ? request.getIsDeleted() : false)
                .build();
    }

    public void updateEntity(UserEntity user, RegisterRequest request) {
        user.setFirstName(request.getFirstName() != null ? request.getFirstName() : user.getFirstName());
        user.setMiddleName(request.getMiddleName() != null ? request.getMiddleName() : user.getMiddleName());
        user.setLastName(request.getLastName() != null ? request.getLastName() : user.getLastName());
        user.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : user.getPhoneNumber());
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : user.getIsActive());
        user.setIsDeleted(request.getIsDeleted() != null ? request.getIsDeleted() : user.getIsDeleted());
    }

    public RegisterResponse toResponse(UserEntity user) {
        return RegisterResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .lastLogin(user.getLastLogin())
                .message("Success")
                .build();
    }
}
