package org.proj.service;

import java.util.List;
import java.util.UUID;

import org.proj.dto.RegisterRequest;
import org.proj.dto.RegisterResponse;
import org.proj.dto.LogoutResponse;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.dto.PasswordResetRequest;

public interface AccountService {

	RegisterResponse register(RegisterRequest request);

	LoginResponse login(LoginRequest request);

	RegisterResponse getAccountById(UUID id, String currentUserEmail);

	List<RegisterResponse> getAllAccounts();

	RegisterResponse updateAccount(UUID id, RegisterRequest request, String currentUserEmail);

	void deleteAccount(UUID id);

	LogoutResponse logout(String userId);

	List<RegisterResponse> filterAccounts(AccountFilterRequest filterRequest);



	void resetPassword(PasswordResetRequest request);

	RegisterResponse getAccountByEmail(String email);

}
