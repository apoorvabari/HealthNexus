package org.proj.service;

import java.util.UUID;
import java.util.List;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.dto.LogoutResponse;
import org.proj.dto.UserFilterRequest;
import org.proj.dto.PasswordResetRequest;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.dto.PageResponse;
import org.proj.entity.UserEntity;

public interface UserService {

	RegisterResponse register(RegisterRequest request);

	LoginResponse login(LoginRequest request);

	RegisterResponse getUserById(UUID id);

	PageResponse<RegisterResponse> getAllUsers(String search, int page, int size);

	RegisterResponse updateUser(UUID id, RegisterRequest request);

	void deleteUser(UUID id);

	LogoutResponse logout();

	List<RegisterResponse> filterUsers(UserFilterRequest filterRequest);

	void updateLastLogin(String email);

	void requestPasswordReset(String email);

	void resetPassword(PasswordResetRequest request);

	void verifyEmail(String token);

	void resendVerification(String email);

	RegisterResponse getUserByEmail(String email);

    UserEntity findUserById(UUID accountId);

    void save(UserEntity user);

    long count();

    long countByIsActiveTrue();

    long countByIsDeletedFalse();

    long countByIsActiveTrueAndIsDeletedFalse();
}
