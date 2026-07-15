package org.proj.controller;

import java.util.List;

import org.proj.dto.AccountRequest;
import org.proj.dto.AccountResponse;
import org.proj.dto.LoginRequest;
import org.proj.dto.LoginResponse;
import org.proj.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

	@Autowired
	private AccountService accountService;

	@PostMapping("/register")

	public ResponseEntity<AccountResponse> registerAccount(
			@RequestBody 
			AccountRequest request) {
		
		AccountResponse response = accountService.register(request);

		return new ResponseEntity<>(response, HttpStatus.CREATED);

	}
	
	@PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody 
            @Valid
            LoginRequest request) {

        return ResponseEntity.ok(accountService.login(request));
    }

	@GetMapping("/{id}")
	public ResponseEntity<AccountResponse> getAccountById(
			@PathVariable 
			Long id) {
	    return ResponseEntity.ok(accountService.getAccountById(id));
	}
	
	@GetMapping
	public ResponseEntity<List<AccountResponse>> getAllAccounts() {
	    return ResponseEntity.ok(accountService.getAllAccounts());
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<AccountResponse> updateAccount(
	        @PathVariable Long id,
	        @RequestBody AccountRequest request) {

	    return ResponseEntity.ok(accountService.updateAccount(id, request));
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteAccount(
			@PathVariable Long id) {

	    accountService.deleteAccount(id);

	    return ResponseEntity.ok("Account deleted successfully");
	}

}
