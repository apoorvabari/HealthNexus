package org.proj.service;

import java.util.List;

import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.dto.LogoutResponse;
import org.proj.dto.AccountFilterRequest;
import org.proj.dto.PasswordResetRequest;

public interface AccountService {

	AccountResponse register(AccountRequest request);

	AccountResponse getAccountById(Long id);

	List<AccountResponse> getAllAccounts();

	AccountResponse updateAccount(Long id, AccountRequest request);

	void deleteAccount(Long id);

	LogoutResponse logout(String userId);

	List<AccountResponse> filterAccounts(AccountFilterRequest filterRequest);

	void updateLastLogin(String email, String keycloakUserId);

	void resetPassword(PasswordResetRequest request);

	AccountResponse getAccountByEmail(String email);

}
