package org.proj.dto;

import lombok.Data;

@Data
public class AccountResponse {
	private Long id;
	private String name ;
	private String email;
	private String role;
	private String message;

}
