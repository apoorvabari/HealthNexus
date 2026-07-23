package org.proj.service;

import java.util.List;

import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.dto.LogoutResponse;
import org.proj.dto.AccountFilterRequest;

public interface AccountService {

	AccountResponse register(AccountRequest request);

	AccountResponse getAccountById(Long id);

	List<AccountResponse> getAllAccounts();

	AccountResponse updateAccount(Long id, AccountRequest request);

	void deleteAccount(Long id);

	LoginResponse login(LoginRequest request);

	LogoutResponse logout(String userId);


	List<AccountResponse> filterAccounts(AccountFilterRequest filterRequest);

}
