package org.proj.mapper;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(
            RegisterRequest request) {

        return UserEntity.builder()
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .profilePicture(
                        request.getProfilePicture()
                )
                .build();
    }

    public void updateEntity(
            UserEntity user,
            RegisterRequest request) {

        if (request.getFirstName() != null) {
            user.setFirstName(
                    request.getFirstName()
            );
        }

        if (request.getMiddleName() != null) {
            user.setMiddleName(
                    request.getMiddleName()
            );
        }

        if (request.getLastName() != null) {
            user.setLastName(
                    request.getLastName()
            );
        }

        if (request.getEmail() != null) {
            user.setEmail(
                    request.getEmail()
            );
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(
                    request.getPhoneNumber()
            );
        }

        if (request.getProfilePicture() != null) {

            String picture =
                    request.getProfilePicture()
                            .trim();

            user.setProfilePicture(
                    picture.isEmpty()
                            ? null
                            : picture
            );
        }

    }

    public RegisterResponse toResponse(
            UserEntity user) {

        return RegisterResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(
                        user.getRole() != null
                                ? user.getRole().getRoleName()
                                : null
                )
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .lastLogin(user.getLastLogin())
                .profilePicture(
                        user.getProfilePicture()
                )
                .message("Success")
                .build();
    }
}
