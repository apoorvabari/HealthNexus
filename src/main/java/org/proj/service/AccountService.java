package org.proj.service;

import java.util.UUID;
import java.util.List;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.dto.LogoutResponse;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.PasswordResetRequest;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;

public interface AccountService {

	RegisterResponse register(RegisterRequest request);

	LoginResponse login(LoginRequest request);

	RegisterResponse getAccountById(UUID id);

	List<RegisterResponse> getAllAccounts();

	RegisterResponse updateAccount(UUID id, RegisterRequest request);

	void deleteAccount(UUID id);

	LogoutResponse logout(String userId);

	List<RegisterResponse> filterAccounts(AccountFilterRequest filterRequest);

	void updateLastLogin(String email, String keycloakUserId);

	void resetPassword(PasswordResetRequest request);

	RegisterResponse getAccountByEmail(String email);

}
