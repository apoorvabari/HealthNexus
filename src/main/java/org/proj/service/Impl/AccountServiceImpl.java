package org.proj.service.Impl;

import java.util.List;

import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.entity.Account;
import org.proj.repository.AccountRepo;
import org.proj.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

	@Autowired
	private AccountRepo accountRepository;

	@Override
	public AccountResponse register(AccountRequest request) {

		if (accountRepository.existsByEmail(request.getEmail())) {
			throw new RuntimeException("Email already exists");
		}

		Account account = new Account();

		account.setName(request.getName());
		account.setEmail(request.getEmail());
		account.setPassword(request.getPassword());
		account.setRole(request.getRole());

		Account savedAccount = accountRepository.save(account);

		AccountResponse response = new AccountResponse();

		response.setId(savedAccount.getId());
		response.setName(savedAccount.getName());
		response.setEmail(savedAccount.getEmail());
		response.setRole(savedAccount.getRole());
		response.setMessage("Account registered successfully");

		return response;
	}

	@Override
	public AccountResponse getAccountById(Long id) {

	    Account account = accountRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Account not found"));

	    AccountResponse response = new AccountResponse();

	    response.setId(account.getId());
	    response.setName(account.getName());
	    response.setEmail(account.getEmail());
	    response.setRole(account.getRole());

	    return response;
	}

	@Override
	public List<AccountResponse> getAllAccounts() {

	    List<Account> accounts = accountRepository.findAll();

	    return accounts.stream().map(account -> {

	        AccountResponse response = new AccountResponse();

	        response.setId(account.getId());
	        response.setName(account.getName());
	        response.setEmail(account.getEmail());
	        response.setRole(account.getRole());

	        return response;

	    }).toList();
	}

	@Override
	public AccountResponse updateAccount(Long id, AccountRequest request) {

	    Account account = accountRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Account not found"));

	    account.setName(request.getName());
	    account.setEmail(request.getEmail());
	    account.setRole(request.getRole());

	    Account updatedAccount = accountRepository.save(account);

	    AccountResponse response = new AccountResponse();

	    response.setId(updatedAccount.getId());
	    response.setName(updatedAccount.getName());
	    response.setEmail(updatedAccount.getEmail());
	    response.setRole(updatedAccount.getRole());
	    response.setMessage("Account updated successfully");

	    return response;
	}

	@Override
	public void deleteAccount(Long id) {

	    if (!accountRepository.existsById(id)) {
	        throw new RuntimeException("Account not found");
	    }

	    accountRepository.deleteById(id);
	}

	@Override
	public LoginResponse login(LoginRequest request) {

	    Account account = accountRepository.findByEmail(request.getEmail())
	            .orElseThrow(() -> new RuntimeException("Invalid email"));

	    if (!account.getPassword().equals(request.getPassword())) {
	        throw new RuntimeException("Invalid password");
	    }

	    LoginResponse response = new LoginResponse();

	    response.setMessage("Login Successful");
	    response.setRole(account.getRole());
	    
	    return response;
	}

}