package org.proj.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data

public class AccountRequest {

	private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Size(min = 4, max = 7, message = "Password between 4 - 7 characters")
    private String password;

    private String role;

	

	

	

}
