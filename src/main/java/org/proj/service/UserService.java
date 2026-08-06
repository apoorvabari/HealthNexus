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

public interface UserService {

	RegisterResponse register(RegisterRequest request);

	LoginResponse login(LoginRequest request);

	RegisterResponse getUserById(UUID id);

	List<RegisterResponse> getAllUsers();

	RegisterResponse updateUser(UUID id, RegisterRequest request);

	void deleteUser(UUID id);

	LogoutResponse logout(String userId);

	List<RegisterResponse> filterUsers(UserFilterRequest filterRequest);

	void updateLastLogin(String email);

	void resetPassword(PasswordResetRequest request);

	RegisterResponse getUserByEmail(String email);

}
